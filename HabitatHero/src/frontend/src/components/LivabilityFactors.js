import LivabilityPreferenceBlock from "../components/LivabilityPreferenceBlock";

function LivabilityFactors({ data, update, showErrors }) {
  const preferences = data.factors;

  const handleModeChange = (factor, mode) => {
    update({
      ...data,
      factors: {
        ...preferences,
        [factor]: {
          ...preferences[factor],
          mode,
          weight: mode === "weighted" ? 0.5 : 0,
        },
      },
    });
  };

  const handleWeightChange = (factor, weight) => {
    update({
      ...data,
      factors: {
        ...preferences,
        [factor]: {
          ...preferences[factor],
          weight: parseFloat(weight),
        },
      },
    });
  };

  const handleConvenienceModeChange = (mode) => {
    update({
      ...data,
      factors: {
        ...preferences,
        convenience: {
          ...preferences.convenience,
          mode,
          weight: mode === "weighted" ? 0.5 : 0,
          selectedAmenities:
            mode === "ignore" ? [] : preferences.convenience.selectedAmenities,
          parentsAddress:
            mode === "ignore" ? "" : preferences.convenience.parentsAddress,
        },
      },
    });
  };

  const handleAmenityToggle = (amenity) => {
    const selectedAmenities = preferences.convenience.selectedAmenities || [];

    const updatedAmenities = selectedAmenities.includes(amenity)
      ? selectedAmenities.filter((item) => item !== amenity)
      : [...selectedAmenities, amenity];

    update({
      ...data,
      factors: {
        ...preferences,
        convenience: {
          ...preferences.convenience,
          selectedAmenities: updatedAmenities,
          parentsAddress:
            amenity === "parentsAddress" && selectedAmenities.includes("parentsAddress")
              ? ""
              : preferences.convenience.parentsAddress,
        },
      },
    });
  };

  const handleParentsAddressChange = (value) => {
    update({
      ...data,
      factors: {
        ...preferences,
        convenience: {
          ...preferences.convenience,
          parentsAddress: value,
        },
      },
    });
  };

  const convenience = preferences.convenience;
  const selectedAmenities = convenience.selectedAmenities || [];
  const parentsAddressSelected = selectedAmenities.includes("parentsAddress");

  const amenityOptions = [
    { key: "parentsAddress", label: "Parents’ Address" },
    { key: "school", label: "Schools" },
    { key: "hawkerCentre", label: "Hawker Centres" },
    { key: "supermarket", label: "Supermarkets" },
    { key: "park", label: "Parks" },
    { key: "hospital", label: "Hospitals" },
    { key: "playground", label: "Playgrounds" }
  ];

  return (
    <div className="step-content">
      <div className="livability-factors-section">
        <LivabilityPreferenceBlock
          title="Solar Orientation"
          subtitle="How much do you want to avoid hot afternoon sun?"
          factorKey="solarOrientation"
          factor={preferences.solarOrientation}
          strictNote="* Units facing strong west sun will be excluded."
          onModeChange={handleModeChange}
          onWeightChange={handleWeightChange}
        />
        {showErrors && !preferences.solarOrientation.mode && (
          <p className="field-error">Please select an option for Solar Orientation.</p>
        )}
      </div>

      <div className="livability-factors-section">
        <LivabilityPreferenceBlock
          title="Acoustic Comfort"
          subtitle="How much do you want to avoid noise?"
          factorKey="acousticComfort"
          factor={preferences.acousticComfort}
          strictNote="* Units within 100m from above-ground MRT tracks & expressways will be excluded."
          onModeChange={handleModeChange}
          onWeightChange={handleWeightChange}
        />
        {showErrors && !preferences.acousticComfort.mode && (
          <p className="field-error">Please select an option for Acoustic Comfort.</p>
        )}
      </div>

      <div className="livability-factors-section">
        <h2>Convenience</h2>
        <p className="question-text">
          How much do you value proximity to key amenities?
        </p>

        <div className="option-row">
          <label className="radio-option">
            <input
              type="radio"
              name="convenience"
              checked={convenience.mode === "ignore"}
              onChange={() => handleConvenienceModeChange("ignore")}
            />
            <span>Ignore</span>
          </label>

          <label className="radio-option">
            <input
              type="radio"
              name="convenience"
              checked={convenience.mode === "strict"}
              onChange={() => handleConvenienceModeChange("strict")}
            />
            <span>Strict Requirement</span>
          </label>

          <label className="radio-option">
            <input
              type="radio"
              name="convenience"
              checked={convenience.mode === "weighted"}
              onChange={() => handleConvenienceModeChange("weighted")}
            />
            <span>Weighted Preference</span>
          </label>
        </div>

        {showErrors && !convenience.mode && (
          <p className="field-error">Please select an option for Convenience.</p>
        )}

        {convenience.mode === "strict" && (
          <p className="warning-note">
            * Units far from selected amenities may be excluded.
          </p>
        )}

        {convenience.mode === "weighted" && (
          <div className="weight-section">
            <div className="weight-value">{convenience.weight.toFixed(1)}</div>

            <div className="weight-slider-container">
              <div className="weight-track"></div>
              <div
                className="weight-fill"
                style={{ width: `${convenience.weight * 100}%` }}
              ></div>

              <input
                type="range"
                min="0"
                max="1"
                step="0.1"
                value={convenience.weight}
                className="weight-slider"
                onChange={(e) =>
                  handleWeightChange("convenience", parseFloat(e.target.value))
                }
              />
            </div>

            <div className="weight-labels">
              <span>0</span>
              <span>1</span>
            </div>
          </div>
        )}

        {(convenience.mode === "strict" || convenience.mode === "weighted") && (
          <>
            <p className="amenities-title">Select Amenities</p>

            <div className="amenity-chip-group">
              {amenityOptions.map((amenity) => (
                <button
                  key={amenity.key}
                  type="button"
                  className={`amenity-chip ${
                    selectedAmenities.includes(amenity.key) ? "selected" : ""
                  }`}
                  onClick={() => handleAmenityToggle(amenity.key)}
                >
                  {amenity.label}
                </button>
              ))}
            </div>

            {showErrors && selectedAmenities.length === 0 && (
              <p className="field-error">Please select at least one amenity.</p>
            )}

            {parentsAddressSelected && (
              <div className="parents-address-container">
                <label className="parents-address-label">
                  Enter your parents' address
                </label>

                <input
                  type="text"
                  className="parents-address-input"
                  value={convenience.parentsAddress}
                  onChange={(e) => handleParentsAddressChange(e.target.value)}
                />
              </div>
            )}

            {showErrors &&
              parentsAddressSelected &&
              convenience.parentsAddress.trim() === "" && (
                <p className="field-error">Please enter your parents' address.</p>
              )}
          </>
        )}
      </div>
    </div>
  );
}

export default LivabilityFactors;