function InputField({ label, type, placeholder, name, value, onChange, ...props }) {
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
        {...props}
      />
    </label>
  );
}

export default InputField;