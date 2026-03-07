import InputField from "../components/InputField";

function CommuterAnalysis({ data, update, showErrors }) {
  const commuters = data.commuters;

  const handleToggleChange = () => {
    update({
      ...data,
      commuters: {
        ...commuters,
        enabled: !commuters.enabled,
      },
    });
  };

  const handleInputChange = (field, value) => {
    update({
      ...data,
      commuters: {
        ...commuters,
        [field]: value,
      },
    });
  };

  return (
    <div className="step-content">
      <div className="multi-commuter-analysis-section">
        <h2>
          Multi-Commuter Analysis<span className="optional"> (optional)</span>
        </h2>

        <p>
          Multi-Commuter Analysis allows the system to evaluate housing options based on the daily travel needs of up to two individuals, balancing commute time and accessibility for both.
        </p>

        <div className="toggle-row">
          <label className="switch">
            <input
              type="checkbox"
              checked={commuters.enabled}
              onChange={handleToggleChange}
            />
            <span className="slider"></span>
          </label>

          <span className={`toggle-label ${commuters.enabled ? "enabled" : "disabled"}`}>
            {commuters.enabled ? "ENABLED" : "DISABLED"}
          </span>
        </div>
      </div>

      {commuters.enabled && (
        <>
          <div className="multi-commuter-analysis-section">
            <InputField
              label="Commuter A Destination"
              type="text"
              name="destA"
              value={commuters.destA}
              onChange={(e) => handleInputChange("destA", e.target.value)}
            />
            {showErrors && commuters.destA.trim() === "" && (
              <p className="field-error">Please enter Commuter A destination.</p>
            )}
          </div>

          <div className="multi-commuter-analysis-section">
            <InputField
              label="Commuter B Destination"
              type="text"
              name="destB"
              value={commuters.destB}
              onChange={(e) => handleInputChange("destB", e.target.value)}
            />
            {showErrors && commuters.destB.trim() === "" && (
              <p className="field-error">Please enter Commuter B destination.</p>
            )}
          </div>

          <div className="multi-commuter-analysis-section">
            <h3>Commute Fairness Priority: {commuters.fairness.toFixed(1)}</h3>

            <input
              type="range"
              min="0"
              max="1"
              step="0.1"
              value={commuters.fairness}
              onChange={(e) =>
                handleInputChange("fairness", parseFloat(e.target.value))
              }
              className="fairness-slider"
            />

            <div className="fairness-labels">
              <span>Low Priority</span>
              <span>High Priority</span>
            </div>

            <div className="fairness-info">
              Fairness is measured as |T_A - T_B|. Lower values indicate more balanced commute times for both commuters.
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default CommuterAnalysis;