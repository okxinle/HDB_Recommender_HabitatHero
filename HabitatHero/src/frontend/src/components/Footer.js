import "../styles/Footer.css";

function Footer() {
  return (
    <footer className="footer">
      <div className="footer-content">
        {/* Mandatory Financial Liability Disclaimer */}
        <p className="disclaimer">
          <strong>Disclaimer:</strong> This platform is an academic prototype for informational purposes only. 
          Matches do not constitute official real estate or financial advice.
        </p>
        
        <div className="footer-links">
          {/* Static Text for Terms and Privacy */}
          <span>Terms & Privacy Policy</span>
          <span>|</span>
          {/* Required Data Citations */}
          <span>© 2026 HabitatHero. Data sources: Data.gov.sg & SLA.</span>
        </div>
      </div>
    </footer>
  );
}

export default Footer;