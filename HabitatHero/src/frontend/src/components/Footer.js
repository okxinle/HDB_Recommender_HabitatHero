import "../styles/Footer.css";
import { Link } from "react-router-dom";

function Footer() {
  return (
    <footer className="footer">
      <div className="footer-content">
        <p className="disclaimer">
          Disclaimer: This platform is an academic prototype for informational purposes only. Matches do not constitute official real estate or financial advice and do not replace official services provided by the Housing and Development Board (HDB).
        </p>

        <div className="footer-links">
          <Link to="/legal#terms">Terms</Link>
          <span>&amp;</span>
          <Link to="/legal#privacy">Privacy Policy</Link>
          <span>|</span>
          <span>© 2026 HabitatHero.</span>
        </div>
      </div>
    </footer>
  );
}

export default Footer;