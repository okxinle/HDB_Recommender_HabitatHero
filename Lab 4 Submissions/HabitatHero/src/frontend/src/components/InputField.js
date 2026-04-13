import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import "../styles/InputField.css";

function InputField({ label, type, placeholder, name, value, onChange, ...props }) {
  const [showPassword, setShowPassword] = useState(false);
  const isPasswordField = type === "password";
  const inputType = isPasswordField && showPassword ? "text" : type;

  return (
    <label className="field">
      <span className="label">{label}</span>
      <div className="input-container">
        <input
          className="input"
          type={inputType}
          placeholder={placeholder}
          name={name}
          value={value}
          onChange={onChange}
          {...props}
        />
        {isPasswordField && (
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="password-toggle-btn"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        )}
      </div>
    </label>
  );
}

export default InputField;