import { useLocation } from "react-router-dom";

function ResultsPage() {
  const location = useLocation();
  const { rankedBlocks } = location.state || { rankedBlocks: [] };

  return (
    <div className="results-container">
      <h1>Your HDB Matches</h1>
      {rankedBlocks.length === 0 ? (
        <p>No matches found. Try adjusting your filters.</p>
      ) : (
        <div className="results-grid">
          {rankedBlocks.map((block, index) => (
            <div key={index} className="result-card">
              <h3>{block.address}</h3>
              <p>Match Score: {block.matchScore}%</p>
              {/* Add more fields from your HDBBlock entity */}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default ResultsPage;