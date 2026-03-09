import { Link } from "react-router-dom";

function QuizSummary({ data }) {
  // Helper to find specific soft constraints in the new List format
  const getFactor = (name) => data.softConstraints.find(f => f.preferenceName === name) || {};

  const formatFactorMode = (factor) => {
    if (factor.mode === "ignore") return "IGNORED";
    if (factor.mode === "strict") return "STRICT REQUIREMENT";
    if (factor.mode === "weighted") return `WEIGHTED PREFERENCE = ${factor.weight.toFixed(1)}`;
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

            <div className="summary-row">
              <span className="summary-key">• Budget</span>
              <span className="summary-value">
                SGD {sc.budgetRange[0].toLocaleString()} – {sc.budgetRange[1].toLocaleString()}
              </span>
            </div>

            <div className="summary-row">
              <span className="summary-key">• Flat Type</span>
              <span className="summary-value">
                {sc.preferredFlatType || "Not selected"}
              </span>
            </div>

            <div className="summary-row">
              <span className="summary-key">• Minimum Lease</span>
              <span className="summary-value">
                {sc.minLeaseYears ? `${sc.minLeaseYears} years` : "Not selected"}
              </span>
            </div>

            <div className="summary-row">
              <span className="summary-key">• Towns</span>
              <span className="summary-value">
                {sc.preferredTowns.length > 0
                  ? sc.preferredTowns.join(", ").replace(/, ([^,]*)$/, " & $1")
                  : "Not selected"}
              </span>
            </div>

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
                  <span className="summary-key">
                    • {key.replace(/([A-Z])/g, " $1").replace(/^./, str => str.toUpperCase()).trim()}
                  </span>
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

            <div className="summary-row">
              <span className="summary-key">• Status</span>
              <span className="summary-value">
                {cp.enabled ? "✔ ENABLED" : "✖ DISABLED"}
              </span>
            </div>

            {cp.enabled && (
              <>
                <div className="summary-row">
                  <span className="summary-key">• Commuter A Destination</span>
                  <span className="summary-value">
                    {cp.destinations[0] || "Not entered"}
                  </span>
                </div>

                <div className="summary-row">
                  <span className="summary-key">• Commuter B Destination</span>
                  <span className="summary-value">
                    {cp.destinations[1] || "Not entered"}
                  </span>
                </div>
              </>
            )}

          </div>
        </div>
        
      </div>
    </div>
  );
}

export default QuizSummary;