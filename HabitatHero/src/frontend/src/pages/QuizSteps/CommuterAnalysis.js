import InputField from "../../components/InputField";

function CommuterAnalysis({ data, update, showErrors }) {
  // 1. Point to the renamed path
  const profile = data.commuterProfile;
  const isValidPostalCode = (value) => /^\d{6}$/.test((value || "").trim());

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
    const sanitizedValue = typeof value === "string" ? value.replace(/\D/g, "").slice(0, 6) : "";

    // Handling the 'destinations' array for REQ-1.5
    if (field === "destA" || field === "destB") {
      const newDests = [...profile.destinations];
      newDests[field === "destA" ? 0 : 1] = sanitizedValue;
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
        <h2>Multi-Commuter Analysis</h2>
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
              label="Commuter A Postal Code"
              value={profile.destinations[0]}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              placeholder="e.g. 670180"
              onChange={(e) => handleInputChange("destA", e.target.value)}
            />
            <p className="field-helper">Enter a 6-digit Singapore postal code</p>
            {showErrors && !isValidPostalCode(profile.destinations[0]) && (
              <p className="field-error">Please enter a valid 6-digit postal code for Commuter A.</p>
            )}
          </div>

          <div className="multi-commuter-analysis-section">
            <InputField
              label="Commuter B Postal Code"
              value={profile.destinations[1]}
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              placeholder="e.g. 560123"
              onChange={(e) => handleInputChange("destB", e.target.value)}
            />
            <p className="field-helper">Enter a 6-digit Singapore postal code</p>
            {showErrors && !isValidPostalCode(profile.destinations[1]) && (
              <p className="field-error">Please enter a valid 6-digit postal code for Commuter B.</p>
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