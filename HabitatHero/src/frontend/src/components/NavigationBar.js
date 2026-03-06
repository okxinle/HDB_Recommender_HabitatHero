import Logo from "../assets/logo.svg";
import "../styles/NavigationBar.css";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";

function NavigationBar() {
  const navigate = useNavigate();

  // 1. State to track if the profile menu is open
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  // 2. Check login state
  const user = JSON.parse(localStorage.getItem("user"));

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setIsMenuOpen(false); // Close menu on logout
    navigate("/login");
  };

return (
    <nav className="navbar">
      <div className="logo">
        <img src={Logo} className="logo-icon" alt="logo" />
        HabitatHero
      </div>

      <div className="nav-links">
        <Link to="/">Home</Link>
        <Link to="/results">Your Results</Link>
        <Link to="/resources">Resources</Link>

        {user ? (
          /* 3. Sleek Profile Dropdown Section */
          <div className="profile-container">
            <button 
              className="profile-trigger" 
              onClick={() => setIsMenuOpen(!isMenuOpen)}
              aria-label="Profile Menu"
            >
              {/* Sleek User Icon SVG */}
              <svg className="user-icon" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08s5.97 1.09 6 3.08c-1.29 1.94-3.5 3.22-6 3.22z"/>
              </svg>
            </button>

            {isMenuOpen && (
              <div className="profile-dropdown">
                <div className="dropdown-header">MY ACCOUNT</div>
                <Link to="/profile" onClick={() => setIsMenuOpen(false)}>Profile Settings</Link>
                <button onClick={handleLogout} className="logout-item">Logout</button>
              </div>
            )}
          </div>
        ) : (
          <Link to="/login" className="login-btn">Login / Sign Up</Link>
        )}
      </div>
    </nav>
  );
}

export default NavigationBar;