import React, { useMemo, useEffect } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Circle, MapContainer, Marker, Popup, Polygon, TileLayer } from 'react-leaflet';
import { DollarSign, Clock, MapPin, BarChart3 } from 'lucide-react';
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
const RESERVE_SITE = [
  [1.3679, 103.8535],
  [1.3687, 103.8553],
  [1.3669, 103.8563],
  [1.3661, 103.8546],
];

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

function SpatialAnalysisResultDashBoardPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { blockId } = useParams();

  const block = location.state?.block ?? {};
  const result = location.state?.result ?? {};

  const blockNumber = block?.blockNumber ?? blockId?.replace('hdb', '') ?? 'N/A';
  const streetName = block?.streetName ?? '';
  const postalCode = block?.postalCode ?? 'N/A';

  const blockLat = pickFirstNumber([block?.coordinates?.lat, block?.coordinates?.latitude]);
  const blockLng = pickFirstNumber([block?.coordinates?.lng, block?.coordinates?.lon, block?.coordinates?.longitude]);
  const hasValidBlockPosition = blockLat !== null && blockLng !== null;
  const blockPosition = hasValidBlockPosition ? [blockLat, blockLng] : [1.3521, 103.8198];

  const matchedAmenities = normalizeSourceObject(result?.matchedAmenities ?? block?.matchedAmenities);
  const matchedAmenitiesEntries = Object.entries(matchedAmenities);
  const nearestAmenities = normalizeSourceObject(result?.nearestAmenities ?? block?.nearestAmenities);
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
    const estimatedPrice = pickFirstNumber([result?.estimatedPrice, block?.estimatedPrice]);
    const floorAreaSqm = pickFirstNumber([result?.floorAreaSqm, block?.floorAreaSqm]);
    const floorAreaSqft = floorAreaSqm === null ? null : floorAreaSqm * 10.7639;

    const townAveragePsf = pickPositiveNumber([
      result?.townAveragePsf,
      block?.townAveragePsf,
      result?.priceInsights?.townAveragePsf,
    ]);

    const legacyTownAveragePrice = pickFirstNumber([
      result?.townAveragePrice,
      result?.avgTownPrice,
      result?.priceInsights?.townAveragePrice,
      block?.townAveragePrice,
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
  }, [block, result]);

  const pedestrianRows = useMemo(
    () => buildPedestrianRows(nearestAmenities, matchedAmenities, blockLat, blockLng),
    [nearestAmenities, matchedAmenities, blockLat, blockLng]
  );

  const nearestMrtDistanceMeters = useMemo(
    () => getNearestDistanceByKeys(nearestAmenities, blockLat, blockLng, ['mrtStation', 'mrt', 'train']),
    [nearestAmenities, blockLat, blockLng]
  );

  const nearestMrtName = useMemo(
    () => getNearestAmenityNameByKeys(nearestAmenities, matchedAmenities, ['mrtStation', 'mrt', 'train']),
    [nearestAmenities, matchedAmenities]
  );

  const hasNearbyMrtSignal = Boolean(
    matchedAmenities?.mrtStation?.length || matchedAmenities?.mrt?.length || matchedAmenities?.train?.length
  );
  const noiseSignature = hasNearbyMrtSignal ? 'Periodic: Train Rumble' : 'Constant: White Noise';

  const peakWindow = block?.westSunStatus ? '2:30 PM - 5:00 PM' : '1:30 PM - 3:30 PM';
  const sunBadgeTone = block?.westSunStatus ? 'moderate' : 'good';
  const noiseBadgeTone = hasNearbyMrtSignal ? 'moderate' : 'good';

  const reserveDistance = hasValidBlockPosition
    ? haversineMeters(blockLat, blockLng, 1.3674, 103.8549)
    : null;

  const viewProtectionScore = reserveDistance === null
    ? 70
    : Math.max(20, Math.min(95, Math.round(100 - reserveDistance / 30)));

  const futureProgressClass = viewProtectionScore >= 80
    ? 'progress-bar__fill progress-bar__fill--good'
    : 'progress-bar__fill progress-bar__fill--moderate';

  const viewRiskCopy = viewProtectionScore >= 80
    ? 'Strong buffer against nearby future development pressure.'
    : 'Moderate exposure to future development changes nearby.';

  const viewRiskSourceCopy = 'Score derived from distance to the highlighted reserve-site polygon.';

  const reserveSite = RESERVE_SITE;

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
            {block?.coordinates
              ? `${block.coordinates.lat.toFixed(6)}, ${block.coordinates.lng.toFixed(6)}`
              : 'N/A'}
          </div>
        </div>
      </section>

      <div className="block-summary-grid">
        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><DollarSign size={18} /></div>
          <div className="summary-content">
            <span>Estimated Price</span>
            <strong>$571,000</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><Clock size={18} /></div>
          <div className="summary-content">
            <span>Lease Remaining</span>
            <strong>57 years</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><MapPin size={18} /></div>
          <div className="summary-content">
            <span>Town</span>
            <strong>Ang Mo Kio</strong>
          </div>
        </div>

        <div className="summary-card-item summary-card-item--row">
          <div className="summary-icon"><BarChart3 size={18} /></div>
          <div className="summary-content">
            <span>Match Score</span>
            <strong>100.0%</strong>
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

            <Polygon
              positions={reserveSite}
              pathOptions={{
                color: '#d8aa43',
                fillColor: '#d8aa43',
                fillOpacity: 0.36,
                weight: 2,
              }}
            />
          </MapContainer>

          <div className="report-map-legend">
            <div className="report-map-legend__item">☀ West Sun Risk</div>
            <div className="report-map-legend__item">🔊 Noise Buffer</div>
            <div className="report-map-legend__item">▧ URA Reserve Site</div>
          </div>
        </div>
      </section>

      <section className="report-grid">
        <article className="intelligence-card">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">☀️ WEST SUN EXPOSURE</h2>
            <span className={`status-badge status-badge--${sunBadgeTone}`}>
              {block?.westSunStatus ? 'MODERATE' : 'GOOD'}
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
          <p className="intelligence-card__text">
            Distance to nearest MRT:{' '}
            <strong>
              {nearestMrtDistanceMeters !== null
                ? `${Math.round(nearestMrtDistanceMeters)}m`
                : nearestMrtName
                  ? `Nearby (${nearestMrtName})`
                  : 'Not detected'}
            </strong>
          </p>
        </article>

        <article className="intelligence-card intelligence-card--future">
          <div className="intelligence-card__header">
            <h2 className="intelligence-card__title">🏗️ FUTURE DEVELOPMENT</h2>
          </div>
          <div className="score-block">
            <div className="score-block__value">{viewProtectionScore}%</div>
            <div className="progress-bar" aria-hidden="true">
              <div className={futureProgressClass} style={{ width: `${viewProtectionScore}%` }} />
            </div>
          </div>
          <p className="intelligence-card__text">{viewRiskCopy}</p>
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