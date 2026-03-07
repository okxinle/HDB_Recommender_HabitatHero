function StructuralConstraints({ data, update, showErrors }) {
  const towns = [
    "Ang Mo Kio",
    "Bedok",
    "Bishan",
    "Bukit Batok",
    "Bukit Merah",
    "Bukit Panjang",
    "Bukit Timah",
    "Central Area",
    "Choa Chu Kang",
    "Clementi",
    "Geylang",
    "Hougang",
    "Jurong East",
    "Jurong West",
    "Kallang/Whampoa",
    "Marine Parade",
    "Pasir Ris",
    "Punggol",
    "Queenstown",
    "Sembawang",
    "Seng Kang",
    "Serangoon",
    "Tampiness",
    "Tengah",
    "Toa Payoh",
    "Woodlands",
    "Yishun"
  ];

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
            <span>SGD 200,000</span>
            <span>SGD 1,000,000</span>
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
              />{" "}
              {town}
            </label>
          ))}
        </div>

        {showErrors && data.towns.length === 0 && (
          <p className="field-error">Please select at least one town.</p>
        )}
      </div>

      <div className="structural-constraints-section">
        <h2>What are your preferred flat types?</h2>

        <div className="flat-type-options">
          {flatTypes.map((type) => (
            <button
              key={type}
              type="button"
              className={`flat-type-btn ${
                data.flatTypes.includes(type) ? "selected" : ""
              }`}
              onClick={() => {
                const updatedTypes = data.flatTypes.includes(type)
                  ? data.flatTypes.filter((t) => t !== type)
                  : [...data.flatTypes, type];

                update({
                  ...data,
                  flatTypes: updatedTypes
                });
              }}
            >
              {type}
            </button>
          ))}
        </div>

        {showErrors && data.flatTypes.length === 0 && (
          <p className="field-error">Please select at least one flat type.</p>
        )}
      </div>

      <div className="structural-constraints-section">
        <h2>What is your preferred minimum lease remaining?</h2>

        <p>
          Minimum lease remaining: {data.minLease} years
        </p>

        <div className="lease-slider">
          <div className="slider-track"></div>

          <input
            type="range"
            min="10"
            max="99"
            step="1"
            value={data.minLease}
            onChange={(e) =>
              update({
                ...data,
                minLease: parseInt(e.target.value),
              })
            }
            className="thumb"
          />
        </div>

        <div className="lease-labels">
          <span>10 years</span>
          <span>99 years</span>
        </div>

        {showErrors && (!data.minLease || data.minLease < 10) && (
          <p className="field-error">
            Please select a minimum lease remaining of at least 10 years.
          </p>
        )}
      </div>
    </div>
  );
}

export default StructuralConstraints;