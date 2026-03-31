import React from "react";
import "../styles/ResourcesPage.css";

const officialResources = [
  {
    title: "HDB Flat Portal",
    description: "Browse official HDB housing options, eligibility details, and market resources in one place.",
    href: "https://www.hdb.gov.sg"
  },
  {
    title: "CPF Housing Grants",
    description: "Understand CPF grant types, grant amounts, and eligibility criteria for first-time and resale buyers.",
    href: "https://www.cpf.gov.sg/member/infohub/educational-resources/a-guide-to-enhanced-cpf-housing-and-proximity-grant"
  },
  {
    title: "Resale Procedure",
    description: "Follow the end-to-end resale process, timelines, document requirements, and official submission steps.",
    href: "https://www.hdb.gov.sg/buying-a-flat/resale-flats/process-for-buying-a-resale-flat/resale-flat-application/application-process"
  }
];

function ResourcesPage() {
  return (
    <main className="resources-page">
      <section className="resources-hero">
        <h1>Housing Resources</h1>
        <p>
          A practical knowledge base for navigating Singapore housing decisions, grants, and resale workflows.
        </p>
      </section>

      <section className="resources-section">
        <div className="section-heading-wrap">
          <h2>Housing Knowledge Base</h2>
          <p>Official HDB and CPF references curated for quick, reliable access.</p>
        </div>

        <div className="resources-grid">
          {officialResources.map((item) => (
            <article key={item.title} className="resource-card">
              <h3>{item.title}</h3>
              <p>{item.description}</p>
              <a href={item.href} target="_blank" rel="noreferrer noopener">
                Visit resource
              </a>
            </article>
          ))}
        </div>
      </section>

      <section className="resources-section technical-showcase">
        <div className="section-heading-wrap">
          <h2>How HabitatHero Works</h2>
          <p>
            HabitatHero uses a smart matching engine to compare thousands of HDB blocks with your lifestyle needs.
          </p>
        </div>

        <div className="showcase-panel">
          <p>
            When you submit your quiz, our recommendation engine evaluates your preferences and ranks homes
            that best fit your goals.
          </p>
          <p>
            The system prioritizes practical factors like commute time and budget so you can focus on homes that suit
            your daily life.
          </p>
          <p>
            Your account information and saved results are handled safely, so your personal data stays private while
            you explore better housing choices.
          </p>
        </div>
      </section>
    </main>
  );
}

export default ResourcesPage;
