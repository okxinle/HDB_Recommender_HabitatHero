import React, { useMemo, useState } from "react";
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

const newsData = [
  {
    id: 1,
    category: "BTO Launches",
    title: "HDB Launches 9,012 Flats in February 2026 BTO and SBF Exercises",
    description: "A summary of the February 2026 BTO and SBF launch details, including supply distribution across towns.",
    image: "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.hdb.gov.sg/hdb-pulse/news/2026/hdb-launches-9012-flats-in-february-2026-bto-and-sbf-exercises"
  },
  {
    id: 2,
    category: "BTO Launches",
    title: "HDB Plans to Launch 19,600 BTO Flats Across Three Sales Exercises in 2026",
    description: "A forward-looking launch roadmap showing how BTO supply is planned across 2026 exercises.",
    image: "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.hdb.gov.sg/hdb-pulse/news/2026/hdb-to-launch-19600-bto-flats-in-2026"
  },
  {
    id: 3,
    category: "Market Trends",
    title: "Resale Price Stabilization: Q4 2025 Sees Slowest Annual Growth Since 2019",
    description: "Q4 resale index movement and what slower annual growth may mean for near-term buyer expectations.",
    image: "https://images.unsplash.com/photo-1560185007-5f0bb1866cab?auto=format&fit=crop&w=1200&q=80",
    link: "https://realestateasia.com/residential/news/singapore-hdb-resale-prices-flat-in-q4-2025-transaction-volumes-fall"
  },
  {
    id: 4,
    category: "Market Trends",
    title: "The $1.5M Milestone: Analysis of the 5-Room Bishan Resale Record",
    description: "Context behind headline resale transactions and how exceptional units influence market sentiment.",
    image: "https://images.unsplash.com/photo-1460317442991-0ec209397118?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.facebook.com/99dotco/posts/a-5-room-dbss-flat-at-natura-loft-has-set-a-new-record-in-bishan-with-s15m-markt/879120000925023/"
  },
  {
    id: 5,
    category: "Land Sales",
    title: "Award of Tender for Media Circle Residential Site (February 2026)",
    description: "Tender outcome update and implications for future residential pipeline in the Media Circle area.",
    image: "https://images.unsplash.com/photo-1484154218962-a197022b5858?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.ura.gov.sg/Corporate/Media-Room/Media-Releases/pr26-05"
  },
  {
    id: 6,
    category: "Land Sales",
    title: "Marina Gardens Lane Site Awarded for Residential Development",
    description: "Summary of awarded site details and what it may signal for housing options in the Marina district.",
    image: "https://images.unsplash.com/photo-1516455590571-18256e5bb9ff?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.ura.gov.sg/Corporate/Media-Room/Media-Releases/pr26-06"
  },
  {
    id: 7,
    category: "Land Sales",
    title: "Holland Drive Tender Results: Boosting Housing Supply in Mature Estates",
    description: "A concise look at the Holland Drive tender outcome and potential supply impact in established estates.",
    image: "https://images.unsplash.com/photo-1472220625704-91e1462799b2?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.ura.gov.sg/Corporate/Media-Room/Media-Releases/pr26-07"
  },
  {
    id: 8,
    category: "Land Sales",
    title: "De Souza Avenue Site Sale: Expanding Residential Options in the West",
    description: "Tender update highlighting western-region development potential and future housing choices.",
    image: "https://images.unsplash.com/photo-1464082354059-27db6ce50048?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.ura.gov.sg/Corporate/Media-Room/Media-Releases/pr26-08"
  },
  {
    id: 9,
    category: "Government Policy",
    title: "URA Leadership Transition: Adele Tan Appointed as New Chief Executive",
    description: "Leadership announcement and what it may mean for planning priorities and long-term urban development.",
    image: "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.ura.gov.sg/Corporate/Media-Room/Media-Releases/pr26-04"
  },
  {
    id: 10,
    category: "Government Policy",
    title: "Extension of Subletting Reprieve for Larger HDB Flats Through 2026",
    description: "Policy extension overview for larger flats and what households should note for subletting plans.",
    image: "https://images.unsplash.com/photo-1430285561322-7808604715df?auto=format&fit=crop&w=1200&q=80",
    link: "https://www.hdb.gov.sg/hdb-pulse/news/2026/extension-of-temporary-relaxation-of-occupancy-cap-for-rental-of-hdb-flats-and-private-residential"
  }
];

function ResourcesPage() {
  const [searchTerm, setSearchTerm] = useState("");

  const normalizedSearch = searchTerm.trim().toLowerCase();

  const filteredKnowledgeBase = officialResources;

  const filteredNewsInsights = useMemo(() => {
    if (!normalizedSearch) return newsData;
    return newsData.filter((item) => {
      const searchBlob = `${item.category} ${item.title}`.toLowerCase();
      return searchBlob.includes(normalizedSearch);
    });
  }, [normalizedSearch]);

  const hasSearchResults = filteredNewsInsights.length > 0;

  return (
    <main className="resources-page">
      <section className="resources-hero">
        <h1>Housing Resources</h1>
        <p>
          A practical knowledge base for navigating Singapore housing decisions, grants, and resale workflows.
        </p>
      </section>

      <section className="search-boundary" aria-label="Search property news">
        <input
          type="text"
          placeholder="Filter guides or news..."
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
      </section>

      {!hasSearchResults && (
        <section className="resources-section no-results-state">
          <h2>No resources found matching your search</h2>
          <p>Try a broader keyword such as grants, resale, budget, or commute.</p>
        </section>
      )}

      <section className="resources-section">
        <div className="section-heading-wrap">
          <h2>Housing Knowledge Base</h2>
          <p>Official HDB and CPF references curated for quick, reliable access.</p>
        </div>

        <div className="resources-grid">
          {filteredKnowledgeBase.map((item) => (
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

      {filteredNewsInsights.length > 0 && (
      <section className="resources-section">
        <div className="section-heading-wrap">
          <h2>Property News</h2>
        </div>

        <div className="resources-grid news-grid">
          {filteredNewsInsights.map((item) => (
            <article key={item.id} className="resource-card news-card">
              <img src={item.image} alt={item.title} className="news-thumb" />
              <span className="news-tag">{item.category}</span>
              <h3>{item.title}</h3>
              <p>{item.description}</p>
              <a href={item.link} className="read-more-btn" target="_blank" rel="noreferrer noopener">
                Read More
              </a>
            </article>
          ))}
        </div>
      </section>
      )}

      <section className="resources-section technical-showcase">
        <div className="section-heading-wrap">
          <h2>Our Methodology & Trust</h2>
          <p>
            We keep our recommendations clear, practical, and focused on what matters to everyday home-seekers.
          </p>
        </div>

        <div className="showcase-panel showcase-grid">
          <div className="showcase-point">
            <h3>Personalized Results</h3>
            <p>
              We look at HDB blocks based on your quiz answers, so the homes you see reflect your goals, routines,
              and the kind of lifestyle you want.
            </p>
          </div>

          <div className="showcase-point">
            <h3>Real-World Factors</h3>
            <p>
              We use trusted official data to help you understand practical details like afternoon sun, nearby train
              lines, and whether travel time feels fair for two people.
            </p>
          </div>

          <div className="showcase-point">
            <h3>Privacy & Security</h3>
            <p>
              Your saved profile details are handled carefully and privately, so you can explore housing options with
              peace of mind.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}

export default ResourcesPage;
