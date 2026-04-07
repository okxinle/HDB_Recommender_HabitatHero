import React, { useMemo, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Circle, MapContainer, Marker, Popup, Polygon, TileLayer } from 'react-leaflet';
import { DollarSign, Clock, MapPin, BarChart3, Ruler } from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import '../styles/SpatialAnalysisResultDashBoardPage.css';

import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

const WALKING_METERS_PER_MINUTE = 80;
const DETAIL_RESULTS_CACHE_KEY = 'detailResultsCache';

const getSafeNumber = (value) => (typeof value === 'number' && Number.isFinite(value) ? value : null);

const pickFirstNumber = (candidates) => {
  for (const candidate of candidates) {
    const parsed = getSafeNumber(candidate);
    if (parsed !== null) {
      return parsed;
    }
  }
  return null;
};

const pickPositiveNumber = (candidates) => {
  for (const candidate of candidates) {
    const parsed = getSafeNumber(candidate);
    if (parsed !== null && parsed > 0) {
      return parsed;
    }
  }
  return null;
};

const formatCurrency = (value) => {
  if (value === null) {
    return 'N/A';
  }

  return new Intl.NumberFormat('en-SG', {
    style: 'currency',
    currency: 'SGD',
    maximumFractionDigits: 0,
  }).format(value);
};

const formatCompactCurrency = (value) => {
  if (value === null) {
    return 'N/A';
  }

  const absoluteValue = Math.abs(Math.round(value));
  if (absoluteValue >= 1000000) {
    return `$${(absoluteValue / 1000000).toFixed(1)}m`;
  }
  if (absoluteValue >= 1000) {
    return `$${Math.round(absoluteValue / 1000)}k`;
  }
  return `$${absoluteValue}`;
};

const formatPercent = (value, digits = 1) => {
  if (value === null) {
    return 'N/A';
  }

  return `${value.toFixed(digits)}%`;
};

const formatLeaseRemaining = (value) => {
  if (value === null) {
    return 'N/A';
  }

  return `${Math.round(value)} years`;
};

const formatFloorArea = (sqm) => {
  if (sqm === null || sqm <= 0) {
    return 'N/A';
  }

  return `${Math.round(sqm * 10.7639)} sqft`;
};

const formatStreet = (value) => {
  if (!value) {
    return '';
  }

  return value.toLowerCase().replace(/(^\w|\s\w)/g, (char) => char.toUpperCase());
};

const formatTown = (town) => {
  if (!town) return 'N/A';

  return town
    .toLowerCase()
    .replace(/(^|\s|\/)\S/g, (char) => char.toUpperCase());
};

const inferDevelopmentIntent = ({ gpr, luDesc, luText }) => {
  const haystack = [gpr, luDesc, luText]
    .filter((value) => typeof value === 'string' && value.trim().length > 0)
    .join(' ')
    .toUpperCase();

  if (!haystack) {
    return 'Unspecified future development plot';
  }

  if (haystack.includes('MRT') || haystack.includes('RAIL') || haystack.includes('TRANSPORT')) {
    return 'Transport infrastructure (possible MRT/rail/road works)';
  }

  if (haystack.includes('RESERVE SITE')) {
    return 'Reserved site (future use not finalized yet)';
  }

  if (haystack.includes('RESIDENTIAL') || haystack.includes('PUBLIC HOUSING') || haystack.includes('HOUSING')) {
    return 'Residential development (possible future housing)';
  }

  if (haystack.includes('COMMERCIAL') || haystack.includes('MIXED USE')) {
    return 'Commercial or mixed-use development';
  }

  if (haystack.includes('PARK') || haystack.includes('OPEN SPACE') || haystack.includes('RECREATION')) {
    return 'Parks or recreational development';
  }

  return 'Planned development zone (URA)';
};


const haversineMeters = (lat1, lon1, lat2, lon2) => {
  const toRad = (deg) => (deg * Math.PI) / 180;
  const earthRadius = 6371000;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadius * c;
};

const parseGeoJsonGeometry = (rawGeom) => {
  if (!rawGeom) {
    return [];
  }

  let geometry = rawGeom;
  if (typeof rawGeom === 'string') {
    try {
      geometry = JSON.parse(rawGeom);
    } catch {
      return [];
    }
  }

  if (!geometry || typeof geometry !== 'object') {
    return [];
  }

  const toLatLngRing = (ring) => {
    if (!Array.isArray(ring)) {
      return null;
    }

    const converted = ring
      .map((point) => {
        if (!Array.isArray(point) || point.length < 2) {
          return null;
        }

        const lng = point[0];
        const lat = point[1];
        if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
          return null;
        }

        return [lat, lng];
      })
      .filter(Boolean);

    return converted.length >= 3 ? converted : null;
  };

  if (geometry.type === 'Polygon') {
    const outerRing = toLatLngRing(geometry.coordinates?.[0]);
    return outerRing ? [outerRing] : [];
  }

  if (geometry.type === 'MultiPolygon') {
    return geometry.coordinates
      .map((polygon) => toLatLngRing(polygon?.[0]))
      .filter(Boolean);
  }

  return [];
};

const isPointInsideRing = (lat, lng, ring) => {
  if (!Array.isArray(ring) || ring.length < 3) {
    return false;
  }

  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const yi = ring[i][0];
    const xi = ring[i][1];
    const yj = ring[j][0];
    const xj = ring[j][1];

    const intersects = ((yi > lat) !== (yj > lat))
      && (lng < ((xj - xi) * (lat - yi)) / ((yj - yi) || Number.EPSILON) + xi);

    if (intersects) {
      inside = !inside;
    }
  }

  return inside;
};

const getMinDistanceToRings = (lat, lng, rings) => {
  if (!Array.isArray(rings) || rings.length === 0) {
    return null;
  }

  let minDistance = null;
  for (const ring of rings) {
    if (!Array.isArray(ring) || ring.length === 0) {
      continue;
    }

    if (isPointInsideRing(lat, lng, ring)) {
      return 0;
    }

    for (const point of ring) {
      if (!Array.isArray(point) || point.length < 2) {
        continue;
      }

      const distance = haversineMeters(lat, lng, point[0], point[1]);
      if (!Number.isFinite(distance)) {
        continue;
      }

      if (minDistance === null || distance < minDistance) {
        minDistance = distance;
      }
    }
  }

  return minDistance;
};

const extractAmenityObject = (value) => {
  if (!value || typeof value !== 'object') {
    return null;
  }

  const name = value.name || value.placeName || value.poiName || value.label || null;
  const address = value.address || value.poiAddress || value.fullAddress || null;
  const streetName = value.streetName || value.street || value.roadName || null;
  const lat = pickFirstNumber([value.lat, value.latitude, value.y]);
  const lng = pickFirstNumber([value.lng, value.lon, value.longitude, value.x]);
  const distanceMeters = pickFirstNumber([value.distanceMeters, value.distance_meters, value.distance]);

  if (!name && !address && !streetName && lat === null && lng === null && distanceMeters === null) {
    return null;
  }

  return {
    name: name || null,
    address,
    streetName,
    lat,
    lng,
    distanceMeters,
  };
};

const normalizeSourceObject = (value) => (value && typeof value === 'object' ? value : {});

const formatAmenityLabel = (key) => {
  const labels = {
    mrtStation: 'MRT Station',
    mrt: 'MRT Station',
    train: 'MRT Station',
    hawkerCentre: 'Hawker Centre',
    hawker: 'Hawker Centre',
    foodCentre: 'Hawker Centre',
    supermarket: 'Supermarket',
    market: 'Supermarket',
    grocery: 'Supermarket',
    school: 'School',
    park: 'Park',
    hospital: 'Hospital',
    playground: 'Playground',
    parentsAddress: "Parents' Home",
  };

  return labels[key] || key;
};

const isMissingName = (value) => {
  const normalized = typeof value === 'string' ? value.trim().toLowerCase() : '';
  if (!normalized) {
    return true;
  }

  return (
    normalized === 'unknown'
    || normalized === 'undefined'
    || normalized === 'null'
    || /^unnamed/.test(normalized)
  );
};

const isHawkerKey = (key) => ['hawkerCentre', 'hawker', 'foodCentre'].includes(key);

const getEstimatedDistanceMetersByAmenityKey = (key) => {
  const estimates = {
    mrtStation: 900,
    mrt: 900,
    train: 900,
    hawkerCentre: 500,
    hawker: 500,
    foodCentre: 500,
    supermarket: 650,
    market: 650,
    grocery: 650,
    school: 800,
    park: 700,
    hospital: 1200,
    playground: 450,
    parentsAddress: 1200,
  };

  return estimates[key] ?? 800;
};

const formatPOIName = (poi) => {
  if (!poi || typeof poi !== 'object') {
    return null;
  }

  const rawName = typeof poi.name === 'string' ? poi.name.trim() : '';
  const address = typeof poi.address === 'string' ? poi.address.trim() : '';
  const streetName = typeof poi.streetName === 'string' ? poi.streetName.trim() : '';
  const amenityKey = poi.amenityKey;

  if (rawName && !isMissingName(rawName)) {
    return rawName;
  }

  if (isHawkerKey(amenityKey)) {
    if (address) {
      return `Hawker Centre at ${address}`;
    }
    if (streetName) {
      return `Hawker Centre near ${streetName}`;
    }
    return null;
  }

  const amenityLabel = formatAmenityLabel(amenityKey);
  if (address) {
    return `${amenityLabel} at ${address}`;
  }
  if (streetName) {
    return `${amenityLabel} near ${streetName}`;
  }

  return null;
};

const buildPedestrianRows = (nearestAmenities, matchedAmenities, blockLat, blockLng) => {
  const rowsFromNearest = Object.entries(normalizeSourceObject(nearestAmenities))
    .flatMap(([key, value]) => {
      const entries = Array.isArray(value) ? value : [value];

      return entries
        .map((entry) => {
          const parsed = extractAmenityObject(entry);
          if (!parsed || !parsed.name) {
            return null;
          }

          let distanceMeters = parsed.distanceMeters;
          if (
            distanceMeters === null &&
            blockLat !== null &&
            blockLng !== null &&
            parsed.lat !== null &&
            parsed.lng !== null
          ) {
            distanceMeters = haversineMeters(blockLat, blockLng, parsed.lat, parsed.lng);
          }

          if (distanceMeters === null || distanceMeters <= 0) {
            const estimatedMeters = getEstimatedDistanceMetersByAmenityKey(key);
            return {
              key,
              label: formatAmenityLabel(key),
              name: formatPOIName({
                ...parsed,
                amenityKey: key,
              }),
              distanceMeters: estimatedMeters,
              walkMinutes: Math.max(1, Math.round(estimatedMeters / WALKING_METERS_PER_MINUTE)),
              isEstimated: true,
            };
          }

          return {
            key,
            label: formatAmenityLabel(key),
            name: formatPOIName({
              ...parsed,
              amenityKey: key,
            }),
            distanceMeters,
            walkMinutes: Math.max(1, Math.round(distanceMeters / WALKING_METERS_PER_MINUTE)),
            isEstimated: false,
          };
        })
        .filter((row) => row && row.name);
    })
    .sort((left, right) => {
      if (left.distanceMeters === null && right.distanceMeters === null) {
        return 0;
      }
      if (left.distanceMeters === null) {
        return 1;
      }
      if (right.distanceMeters === null) {
        return -1;
      }
      return left.distanceMeters - right.distanceMeters;
    });

  const seen = new Set(rowsFromNearest.map((row) => `${row.key}-${row.name}`));

  const rowsFromMatches = Object.entries(normalizeSourceObject(matchedAmenities)).flatMap(([key, names]) => {
    if (!Array.isArray(names)) {
      return [];
    }

    return names
      .map((name) => formatPOIName({ amenityKey: key, name }))
      .filter(Boolean)
      .map((formattedName) => ({
        key,
        label: formatAmenityLabel(key),
        name: formattedName,
        distanceMeters: getEstimatedDistanceMetersByAmenityKey(key),
        walkMinutes: Math.max(1, Math.round(getEstimatedDistanceMetersByAmenityKey(key) / WALKING_METERS_PER_MINUTE)),
        isEstimated: true,
      }))
      .filter((row) => {
        const rowKey = `${row.key}-${row.name}`;
        if (seen.has(rowKey)) {
          return false;
        }
        seen.add(rowKey);
        return true;
      });
  });

  return [...rowsFromNearest, ...rowsFromMatches];
};

const getNearestDistanceByKeys = (nearestAmenities, blockLat, blockLng, keys) => {
  const source = normalizeSourceObject(nearestAmenities);

  let bestMeters = null;
  for (const key of keys) {
    const rawValue = source[key];
    const entries = Array.isArray(rawValue) ? rawValue : [rawValue];

    for (const entry of entries) {
      const parsed = extractAmenityObject(entry);
      if (!parsed) {
        continue;
      }

      let distanceMeters = getSafeNumber(parsed.distanceMeters);
      if (
        distanceMeters === null &&
        blockLat !== null &&
        blockLng !== null &&
        parsed.lat !== null &&
        parsed.lng !== null
      ) {
        distanceMeters = haversineMeters(blockLat, blockLng, parsed.lat, parsed.lng);
      }

      if (distanceMeters === null || distanceMeters <= 0) {
        continue;
      }

      if (bestMeters === null || distanceMeters < bestMeters) {
        bestMeters = distanceMeters;
      }
    }
  }

  return bestMeters;
};

const getNearestAmenityNameByKeys = (nearestAmenities, matchedAmenities, keys) => {
  const source = normalizeSourceObject(nearestAmenities);
  for (const key of keys) {
    const rawValue = source[key];
    const entries = Array.isArray(rawValue) ? rawValue : [rawValue];
    for (const entry of entries) {
      const parsed = extractAmenityObject(entry);
      if (!parsed) {
        continue;
      }

      const formatted = formatPOIName({ ...parsed, amenityKey: key });
      if (formatted) {
        return formatted;
      }
    }
  }

  const matched = normalizeSourceObject(matchedAmenities);
  for (const key of keys) {
    const names = matched[key];
    if (!Array.isArray(names)) {
      continue;
    }

    for (const name of names) {
      const formatted = formatPOIName({ amenityKey: key, name });
      if (formatted) {
        return formatted;
      }
    }
  }

  return null;
};

const sanitizePlaceNames = (key, placeNames, nearestValue) => {
  if (!Array.isArray(placeNames)) {
    placeNames = [];
  }

  let unnamedCount = 0;

  const cleaned = placeNames
    .map((name) => {
      const formatted = formatPOIName({
        amenityKey: key,
        name,
      });

      if (!formatted && isHawkerKey(key) && isMissingName(name)) {
        unnamedCount += 1;
      }

      return formatted;
    })
    .filter((name) => !!name);

  if (cleaned.length === 0 && nearestValue) {
    const nearestEntries = Array.isArray(nearestValue) ? nearestValue : [nearestValue];
    for (const entry of nearestEntries) {
      const parsed = extractAmenityObject(entry);
      if (!parsed) {
        continue;
      }

      const formatted = formatPOIName({
        ...parsed,
        amenityKey: key,
      });
      if (formatted) {
        cleaned.push(formatted);
      }
    }
  }

  if (isHawkerKey(key) && unnamedCount >= 2) {
    cleaned.push('Multiple local food options nearby');
  }

  return cleaned.filter((name, index, self) => self.indexOf(name) === index);
};

const parseRouteBlockId = (rawBlockId) => {
  if (!rawBlockId) {
    return null;
  }

  const match = String(rawBlockId).match(/(\d+)/);
  if (!match) {
    return null;
  }

  const parsed = Number(match[1]);
  return Number.isFinite(parsed) ? parsed : null;
};

const normalizeResultItem = (item) => {
  if (!item || typeof item !== 'object') {
    return {};
  }

  if (item.hdbBlock && typeof item.hdbBlock === 'object') {
    return {
      ...item.hdbBlock,
      ...item,
    };
  }

  return item;
};

function SpatialAnalysisResultDashBoardPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { blockId } = useParams();

  const routeBlockId = useMemo(() => parseRouteBlockId(blockId), [blockId]);
  const cachedResults = useMemo(() => {
    try {
      const parsed = JSON.parse(sessionStorage.getItem(DETAIL_RESULTS_CACHE_KEY) || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }, []);

  const stateBlock = useMemo(
    () => normalizeResultItem(location.state?.hdbBlock ?? location.state?.block ?? location.state?.result),
    [location.state?.hdbBlock, location.state?.block, location.state?.result]
  );

  const fallbackBlock = useMemo(() => {
    if (routeBlockId === null || cachedResults.length === 0) {
      return {};
    }

    const found = cachedResults.find((entry) => {
      const normalized = normalizeResultItem(entry);
      return Number(normalized?.blockId) === routeBlockId;
    });

    return normalizeResultItem(found);
  }, [cachedResults, routeBlockId]);

  const hdbBlock = useMemo(() => {
    if (stateBlock && Object.keys(stateBlock).length > 0) {
      return stateBlock;
    }

    return fallbackBlock;
  }, [stateBlock, fallbackBlock]);

  const resultMeta = useMemo(
    () => normalizeResultItem(location.state?.resultMeta ?? location.state?.result),
    [location.state?.resultMeta, location.state?.result]
  );

  const embeddedFutureDevRisk = resultMeta?.futureDevRisk_500m ?? resultMeta?.futureDevRisk ?? null;
  const [futureDevRisk, setFutureDevRisk] = useState(
    embeddedFutureDevRisk
  );

  const blockNumber = hdbBlock?.blockNumber ?? (routeBlockId === null ? 'N/A' : String(routeBlockId));
  const streetName = hdbBlock?.streetName ?? '';
  const postalCode = hdbBlock?.postalCode ?? 'N/A';

  const summaryStats = useMemo(() => {
    const estimatedPrice = pickFirstNumber([
      hdbBlock?.estimatedPrice,
      resultMeta?.estimatedPrice,
      resultMeta?.priceInsights?.estimatedPrice,
    ]);

    const leaseRemainingYears = pickFirstNumber([
      hdbBlock?.remainingLeaseYears,
      resultMeta?.remainingLeaseYears,
      hdbBlock?.leaseRemaining,
      resultMeta?.leaseRemaining,
    ]);

    const floorAreaSqm = pickFirstNumber([
      hdbBlock?.floorAreaSqm,
      resultMeta?.floorAreaSqm,
    ]);

    const town = formatTown(hdbBlock?.town ?? resultMeta?.town);

    const rawMatchScore = pickFirstNumber([
      hdbBlock?.globalMatchIndex,
      resultMeta?.globalMatchIndex,
      resultMeta?.matchScore,
      hdbBlock?.matchScore,
    ]);

    const normalizedMatchScore =
      rawMatchScore === null
        ? null
        : rawMatchScore <= 1
          ? rawMatchScore * 100
          : rawMatchScore;

    return {
      estimatedPriceText: formatCurrency(estimatedPrice),
      leaseRemainingText: formatLeaseRemaining(leaseRemainingYears),
      floorAreaText: formatFloorArea(floorAreaSqm),
      townText: town,
      matchScoreText: formatPercent(normalizedMatchScore, 1),
    };
  }, [hdbBlock, resultMeta]);

  const blockLat = pickFirstNumber([hdbBlock?.coordinates?.lat, hdbBlock?.coordinates?.latitude]);
  const blockLng = pickFirstNumber([hdbBlock?.coordinates?.lng, hdbBlock?.coordinates?.lon, hdbBlock?.coordinates?.longitude]);
  const hasValidBlockPosition = blockLat !== null && blockLng !== null;
  const blockPosition = hasValidBlockPosition ? [blockLat, blockLng] : [1.3521, 103.8198];

  useEffect(() => {
    if (process.env.NODE_ENV === 'production') {
      return;
    }

    const hasCoordinates = Boolean(hdbBlock?.coordinates);
    const hasAddress = Boolean(hdbBlock?.blockNumber && hdbBlock?.streetName && hdbBlock?.postalCode);
    if (hasCoordinates && !hasAddress) {
      console.warn('Detail-page data inconsistency: coordinates present but address fields missing', {
        blockId: hdbBlock?.blockId,
        blockNumber: hdbBlock?.blockNumber,
        streetName: hdbBlock?.streetName,
        postalCode: hdbBlock?.postalCode,
        coordinates: hdbBlock?.coordinates,
      });
    }
  }, [hdbBlock]);

  useEffect(() => {
    if (embeddedFutureDevRisk) {
      setFutureDevRisk(embeddedFutureDevRisk);
      return;
    }

    const normalizedPostalCode = String(postalCode ?? '').trim();
    if (!normalizedPostalCode || normalizedPostalCode === 'N/A') {
      return;
    }

    const controller = new AbortController();

    const fetchFutureRisk = async () => {
      try {
        const queryParams = new URLSearchParams({
          postalCode: normalizedPostalCode,
          distance: '500',
        });

        if (hasValidBlockPosition) {
          queryParams.set('latitude', String(blockLat));
          queryParams.set('longitude', String(blockLng));
        }

        const response = await fetch(
          `http://localhost:8080/api/hdb/future-development-risk?${queryParams.toString()}`,
          {
            method: 'GET',
            signal: controller.signal,
          }
        );

        const payload = await response.json().catch(() => null);
        if (!response.ok || !payload || payload.status !== 'OK') {
          return;
        }

        setFutureDevRisk(payload);
      } catch (error) {
        if (error?.name !== 'AbortError') {
          console.error('Failed to fetch future development risk:', error);
        }
      }
    };

    fetchFutureRisk();

    return () => controller.abort();
  }, [postalCode, embeddedFutureDevRisk, hasValidBlockPosition, blockLat, blockLng]);

  const futureRiskPolygons = useMemo(() => {
    const developments = Array.isArray(futureDevRisk?.developments)
      ? futureDevRisk.developments
      : [];

    return developments.flatMap((development) => parseGeoJsonGeometry(development?.geom));
  }, [futureDevRisk]);

  const nearestFutureDevelopment = useMemo(() => {
    const developments = Array.isArray(futureDevRisk?.developments)
      ? futureDevRisk.developments
      : [];

    if (developments.length === 0) {
      return null;
    }

    const sorted = [...developments].sort((a, b) => {
      const aDistance = Number(a?.distance_meters);
      const bDistance = Number(b?.distance_meters);

      if (!Number.isFinite(aDistance) && !Number.isFinite(bDistance)) {
        return 0;
      }

      if (!Number.isFinite(aDistance)) {
        return 1;
      }

      if (!Number.isFinite(bDistance)) {
        return -1;
      }

      return aDistance - bDistance;
    });

    return sorted[0] ?? null;
  }, [futureDevRisk]);

  const matchedAmenities = normalizeSourceObject(hdbBlock?.matchedAmenities ?? resultMeta?.matchedAmenities);
  const matchedAmenitiesEntries = Object.entries(matchedAmenities);
  const nearestAmenities = normalizeSourceObject(hdbBlock?.nearestAmenities ?? resultMeta?.nearestAmenities);
  const convenienceRows = useMemo(() => {
    return matchedAmenitiesEntries
      .map(([key, placeNames]) => {
        const cleanedNames = sanitizePlaceNames(key, placeNames, nearestAmenities[key]);
        if (cleanedNames.length === 0) {
          return null;
        }

        return {
          key,
          label: formatAmenityLabel(key),
          names: cleanedNames,
        };
      })
      .filter(Boolean);
  }, [matchedAmenitiesEntries, nearestAmenities]);

  const intelligence = useMemo(() => {
    const estimatedPrice = pickFirstNumber([hdbBlock?.estimatedPrice, resultMeta?.estimatedPrice]);
    const floorAreaSqm = pickFirstNumber([hdbBlock?.floorAreaSqm, resultMeta?.floorAreaSqm]);
    const floorAreaSqft = floorAreaSqm === null ? null : floorAreaSqm * 10.7639;

    const remainingLease = pickFirstNumber([hdbBlock?.remainingLeaseYears, resultMeta?.remainingLeaseYears, resultMeta?.remainingLease, hdbBlock?.remainingLease]);
    const rawScore = pickFirstNumber([hdbBlock?.globalMatchIndex, resultMeta?.globalMatchIndex, resultMeta?.matchScore]);

    const townAveragePsf = pickPositiveNumber([
      hdbBlock?.townAveragePsf,
      resultMeta?.townAveragePsf,
      resultMeta?.priceInsights?.townAveragePsf,
    ]);

    const legacyTownAveragePrice = pickFirstNumber([
      resultMeta?.townAveragePrice,
      resultMeta?.avgTownPrice,
      resultMeta?.priceInsights?.townAveragePrice,
      hdbBlock?.townAveragePrice,
    ]);

    const currentPsf =
      estimatedPrice !== null && floorAreaSqft !== null && floorAreaSqft > 0
        ? estimatedPrice / floorAreaSqft
        : null;

    const normalizedTownAvgPsf =
      townAveragePsf !== null
        ? townAveragePsf
        : legacyTownAveragePrice !== null && floorAreaSqft !== null && floorAreaSqft > 0
          ? legacyTownAveragePrice / floorAreaSqft
          : null;

    const deltaPct =
      currentPsf !== null && normalizedTownAvgPsf !== null && normalizedTownAvgPsf > 0
        ? (normalizedTownAvgPsf - currentPsf) / normalizedTownAvgPsf
        : null;

    const estimatedSavings =
      currentPsf !== null && normalizedTownAvgPsf !== null && floorAreaSqft !== null
        ? (normalizedTownAvgPsf - currentPsf) * floorAreaSqft
        : null;

    const priceBadge =
      deltaPct === null
        ? { label: 'PRICE DATA UNAVAILABLE', tone: 'neutral' }
        : deltaPct > 0.05
          ? { label: '🔥 POTENTIAL BARGAIN', tone: 'positive' }
          : deltaPct < -0.05
            ? { label: '💎 PREMIUM LOCATION', tone: 'premium' }
            : { label: 'FAIR VALUE', tone: 'neutral' };

    const savingsText =
      estimatedSavings === null
        ? 'No town comparison available'
        : estimatedSavings >= 0
          ? `Save ~${formatCompactCurrency(estimatedSavings)} vs town average`
          : `~${formatCompactCurrency(Math.abs(estimatedSavings))} above town average`;

    return {
      remainingLease: remainingLease !== null ? `${remainingLease} years` : 'N/A',
      matchScore: rawScore !== null ? formatPercent(rawScore, 1) : 'N/A',
      estimatedPrice,
      floorAreaSqm,
      floorAreaSqft,
      currentPsf,
      townAveragePsf: normalizedTownAvgPsf,
      deltaPct,
      estimatedSavings,
      priceBadge,
      savingsText,
    };
  }, [hdbBlock, resultMeta]);

  const pedestrianRows = useMemo(
    () => buildPedestrianRows(nearestAmenities, matchedAmenities, blockLat, blockLng),
    [nearestAmenities, matchedAmenities, blockLat, blockLng]
  );

  const hasNearbyMrtSignal = Boolean(
    matchedAmenities?.mrtStation?.length || matchedAmenities?.mrt?.length || matchedAmenities?.train?.length
  );
  const noiseSignature = hasNearbyMrtSignal ? 'Periodic: Train Rumble' : 'Constant: White Noise';

  const peakWindow = hdbBlock?.westSunStatus ? '2:30 PM - 5:00 PM' : '1:30 PM - 3:30 PM';
  const sunBadgeTone = hdbBlock?.westSunStatus ? 'moderate' : 'good';
  const noiseBadgeTone = hasNearbyMrtSignal ? 'moderate' : 'good';

  const reserveDistance = hasValidBlockPosition
    ? getMinDistanceToRings(blockLat, blockLng, futureRiskPolygons)
    : null;

  const viewProtectionScore = reserveDistance === null
    ? null
    : Math.max(20, Math.min(95, Math.round(100 - reserveDistance / 30)));

  const futureProgressClass = viewProtectionScore !== null && viewProtectionScore >= 80
    ? 'progress-bar__fill progress-bar__fill--good'
    : 'progress-bar__fill progress-bar__fill--moderate';

  const hasFutureRiskPolygons = futureRiskPolygons.length > 0;

  const viewRiskCopy = !hasFutureRiskPolygons
    ? 'No mapped future development polygons were found for this block radius.'
    : viewProtectionScore !== null && viewProtectionScore >= 80
      ? 'Strong buffer against nearby future development pressure.'
      : 'Moderate exposure to future development changes nearby.';

  const nearestDistanceText = Number.isFinite(Number(nearestFutureDevelopment?.distance_meters))
    ? `${Math.round(Number(nearestFutureDevelopment.distance_meters))} m`
    : null;

  const nearestGprText = String(nearestFutureDevelopment?.gpr ?? '').trim();
  const nearestLuDescText = String(nearestFutureDevelopment?.lu_desc ?? '').trim();
  const nearestLuTextText = String(nearestFutureDevelopment?.lu_text ?? '').trim();
  const nearestIntentText = inferDevelopmentIntent({
    gpr: nearestGprText,
    luDesc: nearestLuDescText,
    luText: nearestLuTextText,
  });

  const hasDescriptiveData = nearestLuDescText || nearestLuTextText;

  const viewRiskSourceCopy = hasFutureRiskPolygons
    ? `Nearest future planned development zone (URA): ${nearestDistanceText ?? 'distance unavailable'}.`
    : 'No mapped future development zone found within the selected radius.';

  const viewRiskCopyDetailed = hasFutureRiskPolygons && hasDescriptiveData
    ? `Likely nearby change: ${nearestIntentText}.`
    : null;

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  return (
    <div className="spatial-report-page">

      <div className="breadcrumb">
        <span onClick={() => navigate('/results')} className="breadcrumb-link">
          Your Personalized HDB Matches
        </span>
        <span className="breadcrumb-sep">›</span>
        <span>HDB Block Details</span>
      </div>

      <section className="report-block-summary">
        <div className="block-banner">
          Block {blockNumber} {formatStreet(streetName)} Singapore {postalCode}
          <div className="coordinates">
            {hdbBlock?.coordinates
              ? `${hdbBlock.coordinates.lat.toFixed(6)}, ${hdbBlock.coordinates.lng.toFixed(6)}`
              : 'N/A'}
          </div>
        </div>
      </section>

      <div className="block-summary-grid">
        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><DollarSign size={18} /></div>
          <div className="summary-content">
            <span>Estimated Price</span>
            <strong>{summaryStats.estimatedPriceText}</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><Clock size={18} /></div>
          <div className="summary-content">
            <span>Lease Remaining</span>
            <strong>{summaryStats.leaseRemainingText}</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><Ruler size={18} /></div>
          <div className="summary-content">
            <span>Floor Area</span>
            <strong>{summaryStats.floorAreaText}</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><MapPin size={18} /></div>
          <div className="summary-content">
            <span>Town</span>
            <strong>{summaryStats.townText}</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><BarChart3 size={18} /></div>
          <div className="summary-content">
            <span>Match Score</span>
            <strong>{summaryStats.matchScoreText}</strong>
          </div>
        </div>
      </div>

      <section className="report-map-shell">
        <div className="report-map-frame">
          <MapContainer center={blockPosition} zoom={16} className="report-map">
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

            <Marker position={blockPosition}>
              <Popup>
                Block {blockNumber} {formatStreet(streetName)}
              </Popup>
            </Marker>

            <Circle
              center={blockPosition}
              radius={150}
              pathOptions={{
                color: '#dc3545',
                dashArray: '6,6',
                fillColor: '#dc3545',
                fillOpacity: 0.12,
              }}
            />

            {futureRiskPolygons.map((polygon, index) => (
              <Polygon
                key={`future-risk-${index}`}
                positions={polygon}
                pathOptions={{
                  color: '#d8aa43',
                  fillColor: '#d8aa43',
                  fillOpacity: 0.36,
                  weight: 2,
                }}
              />
            ))}
          </MapContainer>

          <div className="report-map-legend">
            <div className="report-map-legend__item">☀ West Sun Risk</div>
            <div className="report-map-legend__item">🔊 Noise Buffer</div>
            <div className="report-map-legend__item">▧ Future Development Risk Zone</div>
          </div>
        </div>
      </section>

      <section className="report-grid">
        <article className="intelligence-card">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">☀️ WEST SUN EXPOSURE</h2>
            <span className={`status-badge status-badge--${sunBadgeTone}`}>
              {hdbBlock?.westSunStatus ? 'MODERATE' : 'GOOD'}
            </span>
          </div>
          <p className="intelligence-card__text">Peak Window: <strong>{peakWindow}</strong></p>
          <p className="intelligence-card__text">HabitatHero Tip: Suggests solar films or black-out curtains.</p>
        </article>

        <article className="intelligence-card">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">🔊 NOISE RISK ASSESSMENT</h2>
            <span className={`status-badge status-badge--${noiseBadgeTone}`}>
              {hasNearbyMrtSignal ? 'MODERATE' : 'GOOD'}
            </span>
          </div>
          <p className="intelligence-card__text">Noise Signature: <strong>{noiseSignature}</strong></p>
        </article>

        <article className="intelligence-card intelligence-card--future">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">🏗️ FUTURE DEVELOPMENT</h2>
          </div>
          <div className="score-block">
            <div className="score-block__value">{viewProtectionScore === null ? 'N/A' : `${viewProtectionScore}%`}</div>
            <div className="progress-bar" aria-hidden="true">
              <div className={futureProgressClass} style={{ width: `${viewProtectionScore ?? 0}%` }} />
            </div>
          </div>
          {viewRiskCopyDetailed && <p className="intelligence-card__text">{viewRiskCopyDetailed}</p>}
          <p className="intelligence-card__text intelligence-card__text--source">{viewRiskSourceCopy}</p>
        </article>

        <article className="intelligence-card intelligence-card--price">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">🔥 PRICE BENCHMARK</h2>
            <span className={`price-badge price-badge--${intelligence.priceBadge.tone}`}>
              {intelligence.priceBadge.label}
            </span>
          </div>
          <div className="price-comparison">
            <div className="price-comparison__row">
              <span>Block psf</span>
              <strong>{intelligence.currentPsf === null ? 'N/A' : formatCurrency(intelligence.currentPsf)}</strong>
            </div>
            <div className="price-comparison__row">
              <span>Town avg psf</span>
              <strong>{intelligence.townAveragePsf === null ? 'N/A' : formatCurrency(intelligence.townAveragePsf)}</strong>
            </div>
          </div>
          <div className="price-savings">
            <span className="price-savings__label">Total Estimated Savings</span>
            <strong className="price-savings__value">{intelligence.savingsText}</strong>
          </div>
        </article>
        <article className="intelligence-card intelligence-card--walk">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">🚶 PEDESTRIAN ACCESS</h2>
            <span className="status-badge status-badge--good">NEARBY ACCESS</span>
          </div>
          {pedestrianRows.length === 0 ? (
            <p className="intelligence-card__text">Walking-time data unavailable for this block.</p>
          ) : (
            <div className="walk-list">
              {pedestrianRows.map((row, index) => (
                <div className="walk-list__item" key={`${row.key}-${row.name}-${index}`}>
                  <div>
                    <div className="walk-list__name">{row.name}</div>
                    <div className="walk-list__label">{row.label}</div>
                  </div>
                  <div className={`walk-list__time${row.walkMinutes === null ? ' walk-list__time--unavailable' : ''}`}>
                    {row.walkMinutes === null ? 'Time unavailable' : `${row.isEstimated ? '~' : ''}${row.walkMinutes} min`}
                  </div>
                </div>
              ))}
            </div>
          )}
        </article>

        {convenienceRows.length > 0 && (
          <article className="intelligence-card">
            <div className="intelligence-card__header">
              <h2 className="intelligence-card__title">🧭 CONVENIENCE FACTORS</h2>
            </div>
            <ul className="convenience-list">
              {convenienceRows.map((row) => {
                return (
                  <li key={row.key} className="convenience-list__item">
                    <strong>{row.label}:</strong>{' '}
                    {row.names.join(', ')}
                  </li>
                );
              })}
            </ul>
          </article>
        )}
      </section>
    </div>
  );
}

export default SpatialAnalysisResultDashBoardPage;