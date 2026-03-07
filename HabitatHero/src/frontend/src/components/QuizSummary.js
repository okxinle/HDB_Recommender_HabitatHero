import { Link } from "react-router-dom";

function QuizSummary({ data }) {
  const formatFactorMode = (factor) => {
    if (!factor || !factor.mode) return "Not Selected";

    if (factor.mode === "ignore") return "Ignored";
    if (factor.mode === "strict") return "Strict Requirement";
    if (factor.mode === "weighted") return `Weighted Preference = ${factor.weight.toFixed(1)}`;

    return factor.mode;
  };

  const getFactorClass = (factor) => {
    if (!factor || !factor.mode) return "tag-default";
    if (factor.mode === "ignore") return "tag-ignore";
    if (factor.mode === "strict") return "tag-strict";
    if (factor.mode === "weighted") return "tag-weighted";
    return "tag-default";
  };

  const selectedBudget = `SGD ${data.budget[0].toLocaleString()} – SGD ${data.budget[1].toLocaleString()}`;
  const selectedFlatTypes =
    data.flatTypes.length > 0 ? data.flatTypes.join(" / ") : "Not selected";
  const selectedTowns =
    data.towns.length > 0 ? data.towns.join(" / ") : "Not selected";

  return (
    <div className="step-content">
        <div className="summary-section">
            <h2 className="summary-title">Summary Of Preferences</h2>

            <div className="summary-future-box">
                <label className="summary-future-label">
                <input type="checkbox" />
                <div>
                    <span className="summary-future-heading">Future Proofing</span>
                    <p className="summary-future-text">
                    Alert me about future construction noise (URA Plans). We’ll notify
                    you if there are planned developments near your chosen blocks that
                    may impact noise levels.
                    </p>
                </div>
                </label>
            </div>
        </div>

        <div className="summary-divider"></div>

        <div className="summary-section">
            <div className="summary-grid">
                <div className="summary-card">
                <div className="summary-card-header">
                    <h3>Structural Constraints</h3>
                    <Link to="/quiz?step=1" className="edit-link">Edit Preferences</Link>
                </div>

                <div className="summary-list">
                    <div className="summary-row">
                    <span className="summary-key">• Budget</span>
                    <span className="summary-value">{selectedBudget}</span>
                    </div>

                    <div className="summary-row">
                    <span className="summary-key">• Flat Types</span>
                    <span className="summary-value">{selectedFlatTypes}</span>
                    </div>

                    <div className="summary-row">
                    <span className="summary-key">• Minimum Lease</span>
                    <span className="summary-value">{data.minLease} years</span>
                    </div>

                    <div className="summary-row">
                    <span className="summary-key">• Towns</span>
                    <span className="summary-value">{selectedTowns}</span>
                    </div>
                </div>
                </div>

                <div className="summary-card">
                <div className="summary-card-header">
                    <h3>Livability Factors</h3>
                    <Link to="/quiz?step=2" className="edit-link">Edit Preferences</Link>
                </div>

                <div className="summary-list">
                    <div className="summary-row">
                    <span className="summary-key">• Solar Orientation</span>
                    <span className={`summary-tag ${getFactorClass(data.factors.solarOrientation)}`}>
                        {formatFactorMode(data.factors.solarOrientation)}
                    </span>
                    </div>

                    <div className="summary-row">
                    <span className="summary-key">• Acoustic Comfort</span>
                    <span className={`summary-tag ${getFactorClass(data.factors.acousticComfort)}`}>
                        {formatFactorMode(data.factors.acousticComfort)}
                    </span>
                    </div>

                    <div className="summary-row">
                    <span className="summary-key">• Convenience</span>
                    <span className={`summary-tag ${getFactorClass(data.factors.convenience)}`}>
                        {formatFactorMode(data.factors.convenience)}
                    </span>
                    </div>
                </div>
                </div>

                <div className="summary-card">
                <div className="summary-card-header">
                    <h3>Multi-Commuter Analysis</h3>
                    <Link to="/quiz?step=3" className="edit-link">Edit Preferences</Link>
                </div>

                <div className="summary-list">
                    <div className="summary-status">
                    {data.commuters.enabled ? "✔ Status: ENABLED" : "✖ Status: DISABLED"}
                    </div>

                    {data.commuters.enabled && (
                    <>
                        <div className="summary-row">
                        <span className="summary-key">• Commuter A Workplace</span>
                        <span className="summary-value">
                            {data.commuters.destA || "Not entered"}
                        </span>
                        </div>

                        <div className="summary-row">
                        <span className="summary-key">• Commuter B Workplace</span>
                        <span className="summary-value">
                            {data.commuters.destB || "Not entered"}
                        </span>
                        </div>

                        <div className="summary-row">
                        <span className="summary-key">• Commute Fairness Priority</span>
                        <span className="summary-value">
                            {data.commuters.fairness.toFixed(1)}
                        </span>
                        </div>
                    </>
                    )}
                </div>
                </div>
            </div>
        </div>
    </div>
  );
}

export default QuizSummary;