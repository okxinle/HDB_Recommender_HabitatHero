import LivabilityPreferenceBlock from "../components/LivabilityPreferenceBlock";

function LivabilityFactors({ data, update, showErrors }) {
  const preferences = data.factors;

  const handleModeChange = (factor, mode) => {
    update({
      ...data,
      factors: {
        ...preferences,
        [factor]: {
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
        <LivabilityPreferenceBlock
          title="Convenience"
          subtitle="How much do you value proximity to key amenities?"
          factorKey="convenience"
          factor={preferences.convenience}
          strictNote="* Units far from MRT stations or key amenities will be excluded."
          onModeChange={handleModeChange}
          onWeightChange={handleWeightChange}
        />
        {showErrors && !preferences.convenience.mode && (
          <p className="field-error">Please select an option for Convenience.</p>
        )}
      </div>
    </div>
  );
}

export default LivabilityFactors;