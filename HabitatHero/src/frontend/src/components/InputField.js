function InputField({ label, type, placeholder, name }) {
  return (
    <label className="field">
      <span className="label">{label}</span>
      <input
        className="input"
        type={type}
        placeholder={placeholder}
        name={name}
      />
    </label>
  );
}

export default InputField;