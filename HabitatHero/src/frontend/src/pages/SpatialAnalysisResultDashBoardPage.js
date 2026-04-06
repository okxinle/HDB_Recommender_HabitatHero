import React, { useEffect } from 'react';
import { useLocation, useNavigate, useParams, Link } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, Circle, Polygon } from 'react-leaflet';
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

const convenienceLabelMap = {
  school: 'School',
  hawkerCentre: 'Hawker Centre',
  supermarket: 'Supermarket',
  park: 'Park',
  hospital: 'Hospital',
  playground: 'Playground',
  parentsAddress: "Parents' Home"
};

const formatTown = (value) =>
  value?.toLowerCase().replace(/(^|\s|\/)\S/g, (char) => char.toUpperCase()) ?? '';

const formatCurrency = (value) => {
  const num = Number(value);
  return value && Number.isFinite(num) ? `$${num.toLocaleString()}` : 'N/A';
};

const formatMatchScore = (value) => {
  const num = Number(value);
  return Number.isFinite(num) ? `${num.toFixed(1)}%` : 'N/A';
};

const formatConvenienceLabel = (key) => convenienceLabelMap[key] || key;

function SpatialAnalysisResultDashBoardPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { blockId } = useParams();

  const block = location.state?.block ?? {};
  const result = location.state?.result ?? {};

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  const blockNumber = block?.blockNumber ?? blockId?.replace('hdb', '') ?? 'N/A';
  const streetName = block?.streetName ?? '';
  const town = block?.town ?? 'N/A';
  const postalCode = block?.postalCode ?? 'N/A';

  const estimatedPrice = result?.estimatedPrice ?? block?.estimatedPrice ?? null;
  const remainingLease = block?.remainingLeaseYears ?? 'N/A';
  const matchScore = result?.globalMatchIndex ?? block?.globalMatchIndex ?? null;

  const blockPosition = [
    block?.coordinates?.lat ?? 1.3696,
    block?.coordinates?.lng ?? 103.8495
  ];

  const matchedAmenities = result?.matchedAmenities ?? block?.matchedAmenities ?? {};
  const matchedAmenitiesEntries = Object.entries(matchedAmenities);

  const reserveSite = [
    [1.3679, 103.8535],
    [1.3687, 103.8553],
    [1.3669, 103.8563],
    [1.3661, 103.8546],
  ];

  return (
    <div className="result-detail-page">

      <div className="breadcrumb">
        <Link to="/results" className="breadcrumb-link">Your Personalized HDB Matches</Link>
        <span className="breadcrumb-separator">&gt;</span>
        <span className="breadcrumb-current">HDB Block Details</span>
      </div>

      <div className="header">
        <h1>HDB Block Details</h1>
        <p>Detailed spatial insights based on your selected match</p>
      </div>

      <div className="block-banner">
        Block {blockNumber} {formatTown(streetName)} {formatTown(town)} Singapore {postalCode}
        <div className="coordinates">
          {block?.coordinates
            ? `${block.coordinates.lat.toFixed(6)}, ${block.coordinates.lng.toFixed(6)}`
            : 'N/A'}
        </div>
      </div>

      <div className="summary-row">
        <div className="summary-item">
          <span>Match Score</span>
          <strong>{formatMatchScore(matchScore)}</strong>
        </div>

        <div className="summary-item">
          <span>Estimated Price</span>
          <strong>{formatCurrency(estimatedPrice)}</strong>
        </div>

        <div className="summary-item">
          <span>Lease Remaining</span>
          <strong>{remainingLease !== 'N/A' ? `${remainingLease} years` : 'N/A'}</strong>
        </div>

        <div className="summary-item">
          <span>Town</span>
          <strong>{formatTown(town)}</strong>
        </div>
      </div>

      <div className="layout">

        <div className="map-section">
          <div className="map-wrapper">

            <MapContainer center={blockPosition} zoom={16} className="map">
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

              <Marker position={blockPosition}>
                <Popup>
                  Block {blockNumber} {formatTown(streetName)}
                </Popup>
              </Marker>

              <Circle
                center={blockPosition}
                radius={150}
                pathOptions={{
                  color: '#e88873',
                  dashArray: '6,6',
                  fillOpacity: 0.1
                }}
              />

              <Polygon
                positions={reserveSite}
                pathOptions={{
                  color: '#d9a431',
                  fillOpacity: 0.4
                }}
              />
            </MapContainer>

            <div className="legend">
              <div className="legend-item">☀ West Sun Risk</div>
              <div className="legend-item">⭕ Noise Risk Buffer</div>
              <div className="legend-item">▧ URA Reserve Site</div>
            </div>

          </div>
        </div>

        <div className="insights">

          <div className="card">
            <h3>WEST SUN EXPOSURE</h3>
            <span className="tag moderate">MODERATE</span>
            <p>Main facade azimuth: 240°</p>
            <p>Classification: South-west facing</p>
          </div>

          <div className="card">
            <h3>NOISE RISK ASSESSMENT</h3>
            <span className="tag low">LOW</span>
            <p>Distance to nearest MRT: 150m</p>
            <p>Noise buffer threshold: 100m</p>
          </div>

          <div className="card">
            <h3>FUTURE DEVELOPMENT RISK</h3>
            <span className="tag moderate">MODERATE</span>
            <ul>
              <li>Nearby URA reserve site</li>
              <li>Temporary construction noise</li>
            </ul>
          </div>

          <div className="card">
            <h3>CONVENIENCE MATCH</h3>
            {matchedAmenitiesEntries.length === 0 ? (
              <p>No matched amenities found.</p>
            ) : (
              <ul>
                {matchedAmenitiesEntries.map(([key, placeNames]) => (
                  <li key={key}>
                    {formatConvenienceLabel(key)}:{' '}
                    {Array.isArray(placeNames) && placeNames.length > 0
                      ? placeNames.join(', ')
                      : 'No places found'}
                  </li>
                ))}
              </ul>
            )}
          </div>

        </div>

      </div>
    </div>
  );
}

export default SpatialAnalysisResultDashBoardPage;