// Export this map so QuizPage.js can use it to auto-fill empty regions during submission
export const REGION_TOWN_MAP = {
  "North": ["Sembawang", "Woodlands", "Yishun"],
  "North-East": ["Ang Mo Kio", "Hougang", "Punggol", "Seng Kang", "Serangoon"],
  "East": ["Bedok", "Pasir Ris", "Tampines"],
  "West": ["Bukit Batok", "Bukit Panjang", "Choa Chu Kang", "Clementi", "Jurong East", "Jurong West", "Tengah"],
  "Central": ["Bishan", "Bukit Merah", "Bukit Timah", "Central Area", "Geylang", "Kallang/Whampoa", "Marine Parade", "Queenstown", "Toa Payoh"]
};

function StructuralConstraints({ data, update, showErrors }) {
  
  // Helper function to update the nested structuralConstraints object
  const updateStructural = (field, value) => {
    update({
      ...data,
      structuralConstraints: {
        ...data.structuralConstraints,
        [field]: value
      }
    });
  };

  // Handles clicking a Region button (e.g., "North")
  const handleRegionToggle = (region) => {
    const currentRegions = data.structuralConstraints.preferredRegions || [];
    const newRegions = currentRegions.includes(region)
      ? currentRegions.filter((r) => r !== region)
      : [...currentRegions, region];
    
    updateStructural("preferredRegions", newRegions);
  };

  // Handles clicking a specific Town button (e.g., "Yishun")
  const handleTownChange = (town) => {
    const currentTowns = data.structuralConstraints.preferredTowns || [];
    const newTowns = currentTowns.includes(town)
      ? currentTowns.filter((t) => t !== town)
      : [...currentTowns, town];

    updateStructural("preferredTowns", newTowns);
  };

  const preferredFlatTypeOptions = ["2-Room", "3-Room", "4-Room", "5-Room", "Executive", "Maisonette"];

  return (
    <div className="step-content">
      {/* 1. BUDGET SECTION */}
      <div className="structural-constraints-section">
        <h2>What is your budget?</h2>
        <div className="budget-slider-container">
          <p>
            Budget Range: SGD {data.structuralConstraints.budgetRange[0].toLocaleString()} – SGD {data.structuralConstraints.budgetRange[1].toLocaleString()}
          </p>

          <div className="budget-slider">
            <div className="slider-track"></div>
            <div
              className="slider-range"
              style={{
                left: `${((data.structuralConstraints.budgetRange[0] - 200000) / (1000000 - 200000)) * 100}%`,
                width: `${((data.structuralConstraints.budgetRange[1] - data.structuralConstraints.budgetRange[0]) / (1000000 - 200000)) * 100}%`,
              }}
            ></div>

            <input
              type="range"
              min="200000"
              max="1000000"
              step="10000"
              value={data.structuralConstraints.budgetRange[0]}
              onChange={(e) =>
                updateStructural("budgetRange", [
                  Math.min(parseInt(e.target.value), data.structuralConstraints.budgetRange[1] - 10000),
                  data.structuralConstraints.budgetRange[1],
                ])
              }
              className="thumb thumb-left"
            />

            <input
              type="range"
              min="200000"
              max="1000000"
              step="10000"
              value={data.structuralConstraints.budgetRange[1]}
              onChange={(e) =>
                updateStructural("budgetRange", [
                  data.structuralConstraints.budgetRange[0],
                  Math.max(parseInt(e.target.value), data.structuralConstraints.budgetRange[0] + 10000),
                ])
              }
              className="thumb thumb-right"
            />
          </div>
        </div>
      </div>

      {/* 2. NEW PREFERRED LOCATION SECTION */}
      <div className="structural-constraints-section">
        <h2>Preferred Location</h2>
        
        {/* Regions Row */}
        <p className="question-text" style={{ fontSize: '15px', marginBottom: '8px' }}>Region</p>
        <div className="flat-type-options" style={{ marginBottom: '20px', flexWrap: 'wrap' }}>
          {Object.keys(REGION_TOWN_MAP).map((region) => {
            const isSelected = (data.structuralConstraints.preferredRegions || []).includes(region);
            return (
              <button
                key={region}
                type="button"
                className={`flat-type-btn ${isSelected ? "selected" : ""}`}
                style={{ 
                  borderRadius: '8px', 
                  padding: '10px 18px', 
                  fontWeight: isSelected ? '600' : '400',
                  background: isSelected ? '#00786e' : '#f5f7f9',
                  color: isSelected ? 'white' : '#333',
                  borderColor: isSelected ? '#00786e' : '#e0e0e0'
                }}
                onClick={() => handleRegionToggle(region)}
              >
                {region}
              </button>
            );
          })}
        </div>

        {/* Dynamic Towns Rows (Shows only for selected regions) */}
        {(data.structuralConstraints.preferredRegions || []).map((region) => (
          <div key={`towns-${region}`} style={{ marginBottom: '24px' }}>
            <p className="question-text" style={{ fontSize: '15px', marginBottom: '10px', color: '#444' }}>
              Towns in {region}
            </p>
            <div className="flat-type-options" style={{ flexWrap: 'wrap', gap: '12px' }}>
              {REGION_TOWN_MAP[region].map((town) => {
                const isTownSelected = (data.structuralConstraints.preferredTowns || []).includes(town);
                return (
                  <button
                    key={town}
                    type="button"
                    className={`flat-type-btn ${isTownSelected ? "selected" : ""}`}
                    style={{ 
                      borderRadius: '8px',
                      background: isTownSelected ? '#e8f5f3' : '#f5f7f9', 
                      borderColor: isTownSelected ? '#00786e' : '#e0e0e0', 
                      color: '#333' 
                    }}
                    onClick={() => handleTownChange(town)}
                  >
                    {town}
                  </button>
                );
              })}
            </div>
            <p style={{ fontSize: '13px', color: '#888', marginTop: '8px' }}>
              Select towns to narrow search, or leave empty to include all towns in {region}
            </p>
          </div>
        ))}

        {showErrors && (data.structuralConstraints.preferredRegions || []).length === 0 && (
          <p className="field-error">Please select at least one region.</p>
        )}
      </div>

      {/* 3. FLAT TYPE SECTION */}
      <div className="structural-constraints-section">
        <h2>What is your preferred flat type?</h2>
        <div className="flat-type-options" style={{ flexWrap: 'wrap' }}>
          {preferredFlatTypeOptions.map((type) => (
            <button
              key={type}
              type="button"
              className={`flat-type-btn ${data.structuralConstraints.preferredFlatType === type ? "selected" : ""}`}
              onClick={() => updateStructural("preferredFlatType", data.structuralConstraints.preferredFlatType === type ? "" : type)}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      {/* 4. LEASE SLIDER SECTION */}
      <div className="structural-constraints-section">
        <h2>What is your preferred minimum lease remaining?</h2>
        <p>Minimum lease remaining: {data.structuralConstraints.minLeaseYears} years</p>
        <div className="lease-slider">
          {/* I added the track back here so it doesn't disappear! */}
          <div className="slider-track"></div> 
          <input
            type="range"
            min="10"
            max="99"
            step="1"
            value={data.structuralConstraints.minLeaseYears}
            onChange={(e) => updateStructural("minLeaseYears", parseInt(e.target.value))}
            className="thumb"
          />
        </div>
      </div>
    </div>
  );
}

export default StructuralConstraints;