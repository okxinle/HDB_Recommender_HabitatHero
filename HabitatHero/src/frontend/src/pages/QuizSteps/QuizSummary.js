import { Link } from "react-router-dom";

export const REGION_TOWN_MAP = {
  "North": ["Sembawang", "Woodlands", "Yishun"],
  "North-East": ["Ang Mo Kio", "Hougang", "Punggol", "Seng Kang", "Serangoon"],
  "East": ["Bedok", "Pasir Ris", "Tampines"],
  "West": ["Bukit Batok", "Bukit Panjang", "Choa Chu Kang", "Clementi", "Jurong East", "Jurong West"],
  "Central": ["Bishan", "Bukit Merah", "Bukit Timah", "Central Area", "Geylang", "Kallang/Whampoa", "Marine Parade", "Queenstown", "Toa Payoh"]
};

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

  const getDisplayedTowns = (structuralConstraints) => {
    const selectedRegions = structuralConstraints.preferredRegions || [];
    const selectedTowns = structuralConstraints.preferredTowns || [];

    const finalTowns = new Set(selectedTowns);

    selectedRegions.forEach((region) => {
      const regionTowns = REGION_TOWN_MAP[region] || [];

      const selectedTownsInThisRegion = regionTowns.filter((town) =>
        finalTowns.has(town)
      );

      if (selectedTownsInThisRegion.length === 0) {
        regionTowns.forEach((town) => finalTowns.add(town));
      }
    });

    return Array.from(finalTowns);
  };

  const sc = data.structuralConstraints;
  const cp = data.commuterProfile;

  const displayedTowns = getDisplayedTowns(sc);

  return (
    <div className="step-content">
      <h2 className="summary-title">Summary Of Preferences</h2>
      <div className="summary-grid">
        
        {/* Structural Constraints Card */}
        <div className="summary-card summary-card-wide">
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

            <div className="summary-row summary-row-towns">
              <span className="summary-key">• Towns</span>
              <span className="summary-value summary-value-towns">
                {displayedTowns.length > 0
                  ? displayedTowns.join(", ").replace(/, ([^,]*)$/, " & $1")
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
            <div className="commuter-status-row">
              <span className={`status-text ${cp.enabled ? "enabled" : "disabled"}`}>
                {cp.enabled ? "✔ Status: ENABLED" : "✖ Status: DISABLED"}
              </span>

              {cp.enabled && (
              <span className="summary-tag tag-fairness">
                COMMUTE FAIRNESS SCORE = {cp.fairnessWeight.toFixed(1)}
              </span>
            )}
            </div>

            {cp.enabled && (
              <>
                <div className="summary-row">
                  <span className="summary-key">• Commuter A Postal Code</span>
                  <span className="summary-value">
                    {cp.destinations[0] || "Not entered"}
                  </span>
                </div>

                <div className="summary-row">
                  <span className="summary-key">• Commuter B Postal Code</span>
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