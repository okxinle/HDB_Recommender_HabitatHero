import { Link } from "react-router-dom";

function QuizSummary({ data }) {
  // Helper to find specific soft constraints in the new List format
  const getFactor = (name) => data.softConstraints.find(f => f.preferenceName === name) || {};

  const formatFactorMode = (factor) => {
    if (factor.mode === "ignore") return "Ignored";
    if (factor.mode === "strict") return "Strict Requirement";
    if (factor.mode === "weighted") return `Weighted Preference = ${factor.weight.toFixed(1)}`;
    return "Not Selected";
  };

  const getFactorClass = (mode) => {
    const classes = { ignore: "tag-ignore", strict: "tag-strict", weighted: "tag-weighted" };
    return classes[mode] || "tag-default";
  };

  const sc = data.structuralConstraints;
  const cp = data.commuterProfile;

  return (
    <div className="step-content">
      <h2 className="summary-title">Summary Of Preferences</h2>
      <div className="summary-grid">
        
        {/* Structural Constraints Card */}
        <div className="summary-card">
          <div className="summary-card-header">
            <h3>Structural Constraints</h3>
            <Link to="/quiz?step=1" className="edit-link">Edit</Link>
          </div>
          <div className="summary-list">
            <p>• Budget: SGD {sc.budgetRange[0].toLocaleString()} – {sc.budgetRange[1].toLocaleString()}</p>
            <p>• Flat Type: {sc.preferredFlatType || "Not selected"}</p>
            <p>• Towns: {sc.preferredTowns.join(" / ") || "Not selected"}</p>
          </div>
        </div>

        {/* Livability Factors Card */}
        <div className="summary-card">
          <div className="summary-card-header">
            <h3>Livability Factors</h3>
            <Link to="/quiz?step=2" className="edit-link">Edit</Link>
          </div>
          <div className="summary-list">
            {["solarOrientation", "acousticComfort", "convenience"].map(key => {
              const f = getFactor(key);
              return (
                <div key={key} className="summary-row">
                  <span>• {key.replace(/([A-Z])/g, ' $1').trim()}</span>
                  <span className={`summary-tag ${getFactorClass(f.mode)}`}>{formatFactorMode(f)}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Commuter Analysis Card */}
        <div className="summary-card">
          <div className="summary-card-header">
            <h3>Multi-Commuter Analysis</h3>
            <Link to="/quiz?step=3" className="edit-link">Edit</Link>
          </div>
          <div className="summary-list">
            <p>Status: {cp.enabled ? "✔ ENABLED" : "✖ DISABLED"}</p>
            {cp.enabled && (
              <>
                <p>• Commuter A: {cp.destinations[0] || "Not entered"}</p>
                <p>• Commuter B: {cp.destinations[1] || "Not entered"}</p>
              </>
            )}
          </div>
        </div>
        
      </div>
    </div>
  );
}

export default QuizSummary;