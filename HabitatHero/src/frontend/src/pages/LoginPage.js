import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import "../styles/LoginPage.css";
import InputField from "../components/InputField";
import HDBAccount from "../assets/hdb_account.png";

const TEMP_RESULTS_KEY = "temporaryGuestResults";
const MEMBER_RESULTS_AVAILABLE_KEY = "memberResultsAvailable";

function LoginPage() {
  const navigate = useNavigate();

// 1. State for form data and error handling
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [errorMessage, setErrorMessage] = useState("");

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // 2. Handle Login Submission (UC-02)
  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");


    try {
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData), 
      });

      const data = await response.json();

      if (data.status === "success") {
        localStorage.setItem("token", data.token);
        localStorage.setItem("user", JSON.stringify(data.user));

        // Login/Results Sync: transfer guest temporary results into persistent member storage.
        const cachedResults = JSON.parse(sessionStorage.getItem(TEMP_RESULTS_KEY) || "[]");
        if (Array.isArray(cachedResults) && cachedResults.length > 0) {
          try {
            await fetch("http://localhost:8080/api/profile/results", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${data.token}`
              },
              body: JSON.stringify(cachedResults)
            });

            sessionStorage.removeItem(TEMP_RESULTS_KEY);
            localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "true");
          } catch (syncError) {
            // Keep temporary results if sync fails so user does not lose state.
            localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "false");
          }
        }

        navigate("/");
      } else {
        setErrorMessage(data.message || "Login failed. Please try again.");
      }
    } catch (error) {
      setErrorMessage("System error. Please try again later.");
    }
  };

return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-left">
          <h1 className="login-title">Welcome Back!</h1>
          <p className="login-subtitle">Login with your email address and password.</p>
          <p className="auth-switch-text auth-switch-top">
            Don’t have an account yet?{" "}
            <Link className="auth-switch-link" to="/create-account">
              Sign up
            </Link>
          </p>

          {/* Display validation errors if any */}
          {errorMessage && <p className="error-text" style={{ color: 'red' }}>{errorMessage}</p>}

          <form className="login-form" onSubmit={handleSubmit}>
            <InputField
              label="Email Address"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleInputChange} // Capturing input
            />

            <InputField
              label="Password"
              type="password"
              name="password"
              value={formData.password}
              onChange={handleInputChange} // Capturing input
            />

            <div className="login-row">
              <label className="remember">
                <input type="checkbox" />
                <span>Keep me logged in</span>
              </label>

            </div>

            <button className="signin-btn" type="submit">
              SIGN IN
            </button>
          </form >
        </div>

        <div className="login-right">
          <img className="login-image" src={HDBAccount} alt="hdb blocks" />
        </div>
      </div>
    </div>
  );
}

export default LoginPage;