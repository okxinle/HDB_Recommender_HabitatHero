function InputField({ label, type, placeholder, name, value, onChange }) {
  return (
    <label className="field">
      <span className="label">{label}</span>
      <input
        className="input"
        type={type}
        placeholder={placeholder}
        name={name}
        value={value}      
        onChange={onChange} 
      />
    </label>
  );
}

export default InputField;