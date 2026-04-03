function LivabilityPreferenceBlock({
  title,
  subtitle,
  factorKey,
  factor,
  strictNote,
  onModeChange,
  onWeightChange,
}) {
  return (
    <div className="preference-block">
      <h2>{title}</h2>
      <p className="question-text">{subtitle}</p>

      <div className="option-row">
        <label className="radio-option">
          <input
            type="radio"
            name={factorKey}
            checked={factor.mode === "ignore"}
            onChange={() => onModeChange(factorKey, "ignore")}
          />
          Ignore
        </label>

        <label className="radio-option">
          <input
            type="radio"
            name={factorKey}
            checked={factor.mode === "strict"}
            onChange={() => onModeChange(factorKey, "strict")}
          />
          Strict Requirement
        </label>

        <label className="radio-option">
          <input
            type="radio"
            name={factorKey}
            checked={factor.mode === "weighted"}
            onChange={() => onModeChange(factorKey, "weighted")}
          />
          Weighted Preference
        </label>
      </div>

      {factor.mode === "strict" && strictNote && (
        <p className="warning-note">{strictNote}</p>
      )}

      {factor.mode === "weighted" && (
        <div className="weight-section">
          <div className="weight-value">{factor.weight.toFixed(1)}</div>

          <div className="weight-slider-container">
            <div className="weight-track"></div>
            <div
              className="weight-fill"
              style={{ width: `${factor.weight * 100}%` }}
            ></div>

            <input
              type="range"
              min="0"
              max="1"
              step="0.1"
              value={factor.weight}
              className="weight-slider"
              onChange={(e) =>
                onWeightChange(factorKey, parseFloat(e.target.value))
              }
            />
          </div>

          <div className="weight-labels">
            <span>0</span>
            <span>1</span>
          </div>
        </div>
      )}
    </div>
  );
}

export default LivabilityPreferenceBlock;