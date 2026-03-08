import InputField from "../components/InputField";

function CommuterAnalysis({ data, update, showErrors }) {
  // 1. Point to the renamed path
  const profile = data.commuterProfile;

  const handleToggleChange = () => {
    update({
      ...data,
      commuterProfile: {
        ...profile,
        enabled: !profile.enabled,
      },
    });
  };

  const handleInputChange = (field, value) => {
    // Handling the 'destinations' array for REQ-1.5
    if (field === "destA" || field === "destB") {
      const newDests = [...profile.destinations];
      newDests[field === "destA" ? 0 : 1] = value;
      update({
        ...data,
        commuterProfile: { ...profile, destinations: newDests }
      });
    } else {
      update({
        ...data,
        commuterProfile: { ...profile, [field]: value }
      });
    }
  };

  return (
    <div className="step-content">
      <div className="multi-commuter-analysis-section">
        <h2>Multi-Commuter Analysis<span className="optional"> (optional)</span></h2>
        <p>Evaluate housing options based on the daily travel needs of up to two individuals.</p>

        <div className="toggle-row">
          <label className="switch">
            <input type="checkbox" checked={profile.enabled} onChange={handleToggleChange} />
            <span className="slider"></span>
          </label>
          <span className={`toggle-label ${profile.enabled ? "enabled" : "disabled"}`}>
            {profile.enabled ? "ENABLED" : "DISABLED"}
          </span>
        </div>
      </div>

      {profile.enabled && (
        <>
          <div className="multi-commuter-analysis-section">
            <InputField
              label="Commuter A Destination"
              value={profile.destinations[0]}
              onChange={(e) => handleInputChange("destA", e.target.value)}
            />
            {showErrors && profile.destinations[0].trim() === "" && (
              <p className="field-error">Please enter Commuter A destination.</p>
            )}
          </div>

          <div className="multi-commuter-analysis-section">
            <InputField
              label="Commuter B Destination"
              value={profile.destinations[1]}
              onChange={(e) => handleInputChange("destB", e.target.value)}
            />
            {showErrors && profile.destinations[1].trim() === "" && (
              <p className="field-error">Please enter Commuter B destination.</p>
            )}
          </div>

          <div className="multi-commuter-analysis-section">
            <h3>Commute Fairness Priority: {profile.fairnessWeight.toFixed(1)}</h3>
            <input
              type="range" min="0" max="1" step="0.1"
              value={profile.fairnessWeight}
              onChange={(e) => handleInputChange("fairnessWeight", parseFloat(e.target.value))}
              className="fairness-slider"
            />
          </div>
        </>
      )}
    </div>
  );
}

export default CommuterAnalysis;