import { useNavigate } from 'react-router-dom';
import '../styles/LegalPage.css';

function LegalPage() {
  const navigate = useNavigate();

  return (
    <div className="legal-page">
      <div className="legal-page__container">
        <button className="legal-page__back" type="button" onClick={() => navigate('/')}>
          Back to Home
        </button>

        <h1 className="legal-page__title">Terms of Use &amp; Privacy Policy</h1>
        <p className="legal-page__intro">
          This page summarizes the legal and data-handling terms for HabitatHero.
        </p>

        <section id="terms" className="legal-page__section">
          <h2>Terms of Use</h2>
          <p>
            HabitatHero is an academic prototype built for informational purposes only. It does not replace
            official services provided by the Housing and Development Board (HDB).
          </p>
          <p>
            Match outputs and related analytics do not constitute official real estate advice or financial advice.
            Users should rely on official channels and qualified professionals for final housing decisions.
          </p>
        </section>

        <section id="privacy" className="legal-page__section">
          <h2>Privacy Policy</h2>
          <p>
            We collect and process registered user email addresses for account registration and authentication.
            We also process address inputs for commute and location-based analysis features.
          </p>
          <p>
            For Guest Users, data is stored only in browser local state/session storage and is purged when the
            session ends. Guest data is not persisted as a registered profile.
          </p>
          <p>
            Registered user passwords are stored using standard cryptographic hashing and are not stored in
            plaintext.
          </p>
        </section>

        <section className="legal-page__section legal-page__section--sources">
          <h2>Data Sources</h2>
          <p>HabitatHero uses public datasets and geospatial references from Data.gov.sg and the Singapore Land Authority (SLA).</p>
        </section>
      </div>
    </div>
  );
}

export default LegalPage;