function LivabilityFactors() {
  return (
    <div className="step-content">
      <h2>Livability Factors</h2>
      <p>Configure your preferences for Solar Orientation and Noise.</p>
      {/* REQ-1.3: Strict vs Weighted Toggle placeholder */}
      <div className="factor-item">
        <span>Solar Orientation (Avoid West Sun)</span>
        <input type="checkbox" /> <span>Strict Requirement</span>
      </div>
    </div>
  );
}

export default LivabilityFactors;