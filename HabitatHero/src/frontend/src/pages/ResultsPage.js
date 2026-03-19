import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import '../styles/ResultsPage.css';

const getSafeNumber = (value) => (typeof value === 'number' && Number.isFinite(value) ? value : null);

const formatMatchScore = (value) => {
  const score = getSafeNumber(value);
  return score === null ? 'N/A Match' : `${score.toFixed(1)}% Match`;
};

const formatCurrency = (value) => {
  const amount = getSafeNumber(value);
  return amount === null ? 'N/A' : `$${amount.toLocaleString()}`;
};

const formatLeaseYears = (value) => {
  const years = getSafeNumber(value);
  return years === null ? 'N/A' : `${years} Years`;
};

function ResultsPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const rankedBlocks = Array.isArray(location.state?.rankedBlocks)
    ? location.state.rankedBlocks
    : [];

  if (rankedBlocks.length === 0) {
    return (
      <div className="no-results-container">
        <h2>No Matches Found</h2>
        <p>We couldn't find any HDBs matching your specific criteria. Try broadening your budget or location!</p>
        <button onClick={() => navigate('/quiz')} className="back-btn">Back to Quiz</button>
      </div>
    );
  }

  return (
    <div className="results-page-wrapper">
      <div className="results-header">
        <h1>Your HDB Matches</h1>
        <p>We found {rankedBlocks.length} blocks tailored to your lifestyle preferences.</p>
      </div>

      <div className="results-grid">
        {rankedBlocks.map((item, index) => {
          const block = item?.hdbBlock ?? item ?? {};
          const blockId = block?.blockId ?? 'N/A';
          const town = block?.town ?? 'Unknown Town';
          const postalCode = block?.postalCode ?? 'N/A';

          const globalMatchIndex = getSafeNumber(item?.globalMatchIndex ?? block?.globalMatchIndex);
          const estimatedPrice = getSafeNumber(item?.estimatedPrice ?? block?.estimatedPrice);
          const remainingLeaseYears = getSafeNumber(block?.remainingLeaseYears);

          const commuteFairnessScore = getSafeNumber(item?.commuteMetrics?.commuteFairnessScore);
          const totalCommuteBurden = getSafeNumber(item?.commuteMetrics?.totalCommuteBurden);
          const fairnessWidth = commuteFairnessScore === null
            ? 0
            : Math.max(0, Math.min(100, commuteFairnessScore * 100));

          const isHighMatch = globalMatchIndex !== null && globalMatchIndex > 75;
          const noiseRiskLevel = block?.noiseRiskLevel ?? '';

          return (
            <div key={`${blockId}-${index}`} className="hdb-card">
              <div className={`match-badge ${isHighMatch ? 'high' : 'med'}`}>
                {formatMatchScore(globalMatchIndex)}
              </div>

              <div className="card-content">
                <h3>{town}</h3>
                <p className="postal-code">Block ID: {blockId} | Postal: {postalCode}</p>

                <hr />

                <div className="details-row">
                  <div className="detail-item">
                    <span>Estimated Price</span>
                    <strong>{formatCurrency(estimatedPrice)}</strong>
                  </div>
                  <div className="detail-item">
                    <span>Lease Remaining</span>
                    <strong>{formatLeaseYears(remainingLeaseYears)}</strong>
                  </div>
                </div>

                {commuteFairnessScore !== null && (
                  <div className="commute-section">
                    <div className="fairness-bar-container">
                      <span className="label">Commute Fairness</span>
                      <div className="fairness-track">
                        <div
                          className="fairness-fill"
                          style={{ width: `${fairnessWidth}%` }}
                        ></div>
                      </div>
                    </div>
                    <p className="commute-time">
                      Total Daily Burden: {totalCommuteBurden ?? 'N/A'} mins
                    </p>
                  </div>
                )}

                <div className="tags-container">
                  {block?.westSunStatus && <span className="tag sun">Afternoon Sun</span>}
                  {String(noiseRiskLevel).toLowerCase() === 'high' && <span className="tag noise">High Noise Risk</span>}
                  {block?.futureRiskFlag && <span className="tag risk">Construction Risk</span>}
                </div>
              </div>

              <button className="view-details-btn">View on Map</button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default ResultsPage;
