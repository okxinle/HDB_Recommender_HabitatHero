import React from 'react';
import { useLocation, useParams } from 'react-router-dom';
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

function SpatialAnalysisResultDashBoardPage() {
  const location = useLocation();
  const { blockId } = useParams();

  const block = location.state?.block ?? {};

  const blockNumber = block?.blockNumber ?? blockId?.replace("hdb", "") ?? "N/A";
  const streetName = block?.streetName ?? "";
  const postalCode = block?.postalCode ?? "N/A";

  const formatStreet = (value) =>
    value?.toLowerCase().replace(/(^\w|\s\w)/g, (char) => char.toUpperCase());

  const blockPosition = [block?.coordinates?.lat, block?.coordinates?.lng];

  const reserveSite = [
    [1.3679, 103.8535],
    [1.3687, 103.8553],
    [1.3669, 103.8563],
    [1.3661, 103.8546],
  ];

  return (
    <div className="result-detail-page">

      <div className="header">
        <h1>Your Personalized HDB Matches</h1>
        <p>Ranked by lifestyle compatibility based on your preferences.</p>
      </div>

      <div className="block-banner">
        Block {blockNumber} {formatStreet(streetName)} Singapore {postalCode}
      </div>

      <p>
        <strong>Coordinates:</strong>{" "}
        {block?.coordinates? `${block.coordinates.lat}, ${block.coordinates.lng}`:"N/A"}
      </p>

      <div className="layout">

        <div className="map-section">
          <div className="map-wrapper">

            <MapContainer
              center={blockPosition}
              zoom={16}
              className="map"
            >
              <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />

              <Marker position={blockPosition}>
                <Popup>Block {blockNumber} {formatStreet(streetName)}</Popup>
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

        </div>

      </div>
    </div>
  );
}

export default SpatialAnalysisResultDashBoardPage;