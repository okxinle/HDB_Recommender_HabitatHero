function StructuralConstraints({ data, update }) {
  const towns = ["Ang Mo Kio", "Bedok", "Bishan", "Clementi", "Punggol", "Toa Payoh"];

  const handleTownChange = (town) => {
    const newTowns = data.towns.includes(town)
      ? data.towns.filter((t) => t !== town)
      : [...data.towns, town];
    update({ ...data, towns: newTowns });
  };

  return (
    <div className="step-content">
      <h2>What is your budget?</h2>
      <p>Budget Range: SGD {data.budget[0]} - SGD {data.budget[1]}</p>
      {/* Simulation of REQ-1.1: Budget Filter */}
      <input 
        type="range" min="200000" max="1000000" step="10000"
        value={data.budget[1]}
        onChange={(e) => update({...data, budget: [200000, parseInt(e.target.value)]})}
      />

      <h2>What are your preferred towns?</h2>
      <div className="town-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
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
  );
}

export default StructuralConstraints;