import { Link } from "react-router-dom";
import "../styles/LoginPage.css";
import InputField from "../components/InputField";
import HDBAccount from "../assets/hdb_account.png";

function LoginPage() {

  const handleSubmit = (e) => {
    e.preventDefault();
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-left">
          <h1 className="login-title">Welcome Back!</h1>
          <p className="login-subtitle">Login with your email address and password.</p>

          <form className="login-form" onSubmit={handleSubmit}>
            <InputField
              label="Email Address"
              type="email"
              name="email"
            />

            <InputField
              label="Password"
              type="password"
              name="password"
            />

            <div className="login-row">
              <label className="remember">
                <input type="checkbox" />
                <span>Keep me logged in</span>
              </label>

              <Link className="forgot">
                Forgot password?
              </Link>
            </div>

            <p className="signup-hint">
              <Link className="signup-link" to="/create-account">
                Don’t have an account yet? Sign up here.
              </Link>
            </p>

            <button className="signin-btn" type="submit">
              SIGN IN
            </button>
          </form>
        </div>

        <div className="login-right">
          <img className="login-image" src={HDBAccount} alt="hdb blocks" />
        </div>
      </div>
    </div>
  );
}

export default LoginPage;