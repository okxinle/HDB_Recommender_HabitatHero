import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import "../styles/LoginPage.css";
import InputField from "../components/InputField";
import HDBAccount from "../assets/hdb_account.png";

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

    /* ============================================================
    PRODUCTION CODE (Commented Out for Future Use)
    ============================================================
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData), 
      });

      const data = await response.json();

      if (data.status === "success") {
        localStorage.setItem("token", data.token);
        localStorage.setItem("user", JSON.stringify(data.user));
        navigate("/");
      } else {
        setErrorMessage(data.message || "Login failed. Please try again.");
      }
    } catch (error) {
      setErrorMessage("System error. Please try again later.");
    }
    */

    // ============================================================
    // MOCK TESTING LOGIC (for Testing Now)
    // ============================================================
    // Test Credentials: user@example.com / password123
    if (formData.email === "user@example.com" && formData.password === "123") {
      
      const mockSuccessData = {
        status: "success",
        token: "jwt-session-token-abc", // Matches your UC-02 contract
        user: {
          userID: 1042,
          email: "user@example.com",
          isActive: true
        }
      };

      // Store in localStorage so NavigationBar can read it
      localStorage.setItem("token", mockSuccessData.token);
      localStorage.setItem("user", JSON.stringify(mockSuccessData.user));

      // Redirect to Homepage
      navigate("/");
    } else {
      // Simulate "The Universal Error Response"
      setErrorMessage("Invalid credentials. Try: user@example.com / 123");
    }
    // ============================================================
  };

return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-left">
          <h1 className="login-title">Welcome Back!</h1>
          <p className="login-subtitle">Login with your email address and password.</p>

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

            <p className="signup-hint">
              {/* This links to your sign-up requirement (UC-01) */}
              <Link className="signup-link" to="/create-account">
                Don’t have an account yet? Sign up here.
              </Link>
            </p>

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