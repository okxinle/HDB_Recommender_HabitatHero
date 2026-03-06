import "../styles/HomePage.css";
import HDBHome from "../assets/hdb_home.png";
import RatingCard from "../components/RatingCard";
import FeatureCard from "../components/FeatureCard";
import { Link } from "react-router-dom"; // Add this line

function HomePage() {
  const ratings = ["Usefulness", "Reliability", "Accuracy"];

  const features = [
    {
      title: "Lifestyle First",
      description: "We focus on how you live, not just where you live.",
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 3" />
        </svg>
      ),
    },
    {
      title: "Smart Scoring",
      description: "We calculate a 0–100% match score for every block.",
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 17l6-6 4 4 7-7" />
          <path d="M14 8h7v7" />
        </svg>
      ),
    },
    {
      title: "Future Vision",
      description: "We warn you about future construction risks.",
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      ),
    },
    {
      title: "Transparent Ranking",
      description: "We explain why blocks rank higher or lower.",
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M12 2l3 7h7l-5.5 4.2 2 7-6.5-4.5-6.5 4.5 2-7L2 9h7z" />
        </svg>
      ),
    },
    {
      title: "Fair Commutes",
      description: "We balance travel times for one or two daily commuters.",
      icon: (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 12a9 9 0 0 1 15-6" />
          <polyline points="18 3 18 9 12 9" />
          <path d="M21 12a9 9 0 0 1-15 6" />
          <polyline points="6 21 6 15 12 15" />
        </svg>
      ),
    },
  ];
  return (
    <div className="home">
      <section className="cover">
        <div className="cover-left">
          <h1>
            Don’t just find a house.<br />
            <span className="highlight">Find a habitat.</span>
          </h1>

          <p>We translate your lifestyle needs into a personalized HDB compatibility score.</p>

          <div className="ratings">
            {ratings.map((label) => (
              <RatingCard key={label} label={label} />
            ))}

          </div>
          <Link to="/quiz">
          <button className="primary-btn">Start Lifestyle Quiz</button>
          </Link>
        </div>

        <div className="cover-right">
          <img src={HDBHome} alt="HDB Background" className="cover-image" />
        </div>
      </section>

      <section className="features">
        {features.map((f) => (
          <FeatureCard key={f.title} icon={f.icon} title={f.title} description={f.description} />
        ))}
      </section>
    </div>
  );
}

export default HomePage;