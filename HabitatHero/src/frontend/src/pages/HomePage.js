import "../styles/HomePage.css";
import HDBHome from "../assets/hdb_home.png";
import RatingCard from "../components/RatingCard";
import FeatureCard from "../components/FeatureCard";

function HomePage() {
  const ratings = ["Usefulness", "Reliability", "Accuracy"];

  const features = [
    { title: "Lifestyle First", description: "We focus on how you live, not just where you live." },
    { title: "Smart Scoring", description: "We calculate a 0–100% match score for every block." },
    { title: "Future Vision", description: "We warn you about future construction risks." },
    { title: "Transparent Ranking", description: "We explain why blocks rank higher or lower." },
    { title: "Fair Commutes", description: "We balance travel times for one or two daily commuters." }
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

          <button className="primary-btn">Start Lifestyle Quiz</button>
        </div>

        <div className="cover-right">
          <img src={HDBHome} alt="HDB Background" className="cover-image" />
        </div>
      </section>

      <section className="features">
        {features.map((f) => (
          <FeatureCard key={f.title} title={f.title} description={f.description} />
        ))}
      </section>
    </div>
  );
}

export default HomePage;