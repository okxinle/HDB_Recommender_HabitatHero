import Logo from "../assets/logo.svg";
import "../styles/NavigationBar.css";
import { Link } from "react-router-dom";

function NavigationBar() {
  return (
    <nav className="navbar">
      <div className="logo">
        <img src={Logo} className="logo-icon" alt="logo" />
        HabitatHero
      </div>

      <div className="nav-links">
        <Link to="/">Home</Link>
        <Link to="/">Your Results</Link>
        <Link to="/">Resources</Link>
        <Link to="/">New Projects</Link>

        <button className="search-btn" aria-label="Search">
          <svg
            className="search-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="7" />
            <line x1="16.65" y1="16.65" x2="21" y2="21" />
          </svg>
        </button>

        <Link to="/login" className="login-btn">Login / Sign Up</Link>
      </div>
    </nav>
  );
}

export default NavigationBar;