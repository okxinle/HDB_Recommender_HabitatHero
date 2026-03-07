function StructuralConstraints({ data, update }) {
  const towns = ["Ang Mo Kio", "Bedok", "Bishan", "Bukit Batok", "Bukit Merah", "Bukit Panjang", "Bukit Timah", "Central Area", "Choa Chu Kang", "Clementi", "Geylang", "Hougang", "Jurong East", "Jurong West", "Kallang/Whampoa", "Marine Parade", "Pasir Ris", "Punggol", "Queenstown", "Sembawang", "Seng Kang", "Serangoon", "Tampiness", "Tengah", "Toa Payoh", "Woodlands", "Yishun"];

  const handleTownChange = (town) => {
    const newTowns = data.towns.includes(town)
      ? data.towns.filter((t) => t !== town)
      : [...data.towns, town];
    update({ ...data, towns: newTowns });
  };

  const flatTypes = [
    "2-Room",
    "3-Room",
    "4-Room",
    "5-Room",
    "Executive",
    "Maisonette"
  ];

  return (
    <div className="step-content">
      <div className="structural-constraints-section">
        <h2>What is your budget?</h2>
        <div className="budget-slider-container">
          <p>
            Budget Range: SGD {data.budget[0].toLocaleString()} – SGD {data.budget[1].toLocaleString()}
          </p>

          <div className="budget-slider">
            <div className="slider-track"></div>
            <div
              className="slider-range"
              style={{
                left: `${((data.budget[0] - 200000) / (1000000 - 200000)) * 100}%`,
                width: `${((data.budget[1] - data.budget[0]) / (1000000 - 200000)) * 100}%`,
              }}
            ></div>

            <input
              type="range"
              min="200000"
              max="1000000"
              step="10000"
              value={data.budget[0]}
              onChange={(e) =>
                update({
                  ...data,
                  budget: [
                    Math.min(parseInt(e.target.value), data.budget[1] - 10000),
                    data.budget[1],
                  ],
                })
              }
              className="thumb thumb-left"
            />

            <input
              type="range"
              min="200000"
              max="1000000"
              step="10000"
              value={data.budget[1]}
              onChange={(e) =>
                update({
                  ...data,
                  budget: [
                    data.budget[0],
                    Math.max(parseInt(e.target.value), data.budget[0] + 10000),
                  ],
                })
              }
              className="thumb thumb-right"
            />
          </div>

          <div className="budget-labels">
            <span>$200k</span>
            <span>$1M</span>
          </div>
        </div>
      </div>

      <div className="structural-constraints-section">
        <h2>What are your preferred towns?</h2>
        <div className="town-grid">
          {towns.map((town) => (
            <label key={town}>
              <input 
                type="checkbox" 
                checked={data.towns.includes(town)} 
                onChange={() => handleTownChange(town)} 
              /> {town}
            </label>
          ))}
        </div>
      </div>

      <div className="structural-constraints-section">
        <h2>What is your preferred flat type?</h2>
        <div className="flat-type-options">
          {flatTypes.map((type) => (
            <button
              key={type}
              className={`flat-type-btn ${
                data.flatType === type ? "selected" : ""
              }`}
              onClick={() =>
                update({
                  ...data,
                  flatType: type
                })
              }
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      <div className="structural-constraints-section">
        <h2>What is your preferred minimum lease remaining?</h2>
        <p>
          Minimum lease remaining: {data.lease} years
        </p>

        <div className="lease-slider">
          <div className="slider-track"></div>

          <input
            type="range"
            min="10"
            max="99"
            step="1"
            value={data.lease}
            onChange={(e) =>
              update({
                ...data,
                lease: parseInt(e.target.value),
              })
            }
            className="thumb"
          />
        </div>
      </div>

      <div className="lease-labels">
        <span>10 years</span>
        <span>99 years</span>
      </div>
    </div>
  );
}

export default StructuralConstraints;