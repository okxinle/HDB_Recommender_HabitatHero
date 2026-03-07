import { useState } from "react";
import LivabilityPreferenceBlock from "../components/LivabilityPreferenceBlock";

function LivabilityFactors() {
  const [preferences, setPreferences] = useState({
    solarOrientation: {
      mode: "ignore",
      weight: 0,
    },
    acousticComfort: {
      mode: "ignore",
      weight: 0,
    },
    convenience: {
      mode: "ignore",
      weight: 0,
    },
  });

  const handleModeChange = (factor, mode) => {
    setPreferences((prev) => ({
      ...prev,
      [factor]: {
        mode,
        weight: mode === "weighted" ? 0.5 : 0,
      },
    }));
  };

  const handleWeightChange = (factor, weight) => {
    setPreferences((prev) => ({
      ...prev,
      [factor]: {
        ...prev[factor],
        weight: parseFloat(weight),
      },
    }));
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
      </div>
    </div>
  );
}

export default LivabilityFactors;