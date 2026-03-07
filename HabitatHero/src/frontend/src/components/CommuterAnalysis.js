import { useState } from "react";
import InputField from "../components/InputField";

function CommuterAnalysis() { 

  const [enabled, setEnabled] = useState(false);
  const [commuterA, setCommuterA] = useState("");
  const [commuterB, setCommuterB] = useState("");
  const [fairness, setFairness] = useState(0.5);
    
  return (
    <div className="step-content">
      <div className="multi-commuter-analysis-section">
        <h2>Multi-Commuter Analysis (optional)</h2>
        <p>
          Multi-Commuter Analysis allows the system to evaluate housing options based on the daily travel needs of up to two individuals, balancing commute time and accessibility for both.
        </p>

        <div className="toggle-row">
          <label className="switch">
            <input
              type="checkbox"
              checked={enabled}
              onChange={() => setEnabled(!enabled)}
            />
            <span className="slider"></span>
          </label>

          <span className={`toggle-label ${enabled ? "enabled" : "disabled"}`}>
            {enabled ? "ENABLED" : "DISABLED"}
            </span>
        </div>
      </div>

      {enabled && (
        <>
            <div className="multi-commuter-analysis-section">
                <InputField
                label="Commuter A Destination"
                type="text"
                name="commuterA"
                value={commuterA}
                onChange={(e) => setCommuterA(e.target.value)}
                />
            </div>

            <div className="multi-commuter-analysis-section">
                <InputField
                label="Commuter B Destination"
                type="text"
                name="commuterB"
                value={commuterB}
                onChange={(e) => setCommuterB(e.target.value)}
                />
            </div>

            <div className="multi-commuter-analysis-section">
                <h3>Commute Fairness Priority: {fairness.toFixed(1)}</h3>

                <input
                type="range"
                min="0"
                max="1"
                step="0.1"
                value={fairness}
                onChange={(e) => setFairness(parseFloat(e.target.value))}
                className="fairness-slider"
                />

                <div className="fairness-labels">
                <span>Low Priority</span>
                <span>High Priority</span>
                </div>

                <div className="fairness-info">
                Fairness is measured as |T_A - T_B|. Lower values indicate more balanced commute times for both partners.
                </div>
            </div>
        </>
      )}

    </div>
  );
}

export default CommuterAnalysis;