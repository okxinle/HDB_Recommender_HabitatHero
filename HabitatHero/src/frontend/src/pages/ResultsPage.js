import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { SearchX } from 'lucide-react';
import '../styles/ResultsPage.css';

const TEMP_RESULTS_KEY = 'temporaryGuestResults';
const MEMBER_RESULTS_AVAILABLE_KEY = 'memberResultsAvailable';

const getSafeNumber = (value) => (typeof value === 'number' && Number.isFinite(value) ? value : null);

const formatMatchScore = (value) => {
  const score = getSafeNumber(value);
  return score === null ? 'N/A Match' : `${score.toFixed(1)}% Match`;
};

const formatCurrency = (value) => {
  const amount = getSafeNumber(value);
  return amount === null ? 'N/A' : `$${amount.toLocaleString()}`;
};

const formatLeaseYears = (value) => {
  const years = getSafeNumber(value);
  return years === null ? 'N/A' : `${years} Years`;
};

function ResultsPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const token = localStorage.getItem('token') || '';
  const user = localStorage.getItem('user');
  const isAuthenticated = Boolean(token && user);

  const [memberRankedBlocks, setMemberRankedBlocks] = useState([]);
  const [isLoadingMemberResults, setIsLoadingMemberResults] = useState(false);

  const cachedRankedBlocks = (() => {
    try {
      const parsed = JSON.parse(sessionStorage.getItem(TEMP_RESULTS_KEY) || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return [];
    }
  })();

  const stateRankedBlocks = Array.isArray(location.state?.rankedBlocks)
    ? location.state.rankedBlocks
    : null;

  useEffect(() => {
    // Guests: no backend fetch needed.
    if (!isAuthenticated) return;

    // If we just navigated from quiz with fresh results, show them immediately.
    if (Array.isArray(stateRankedBlocks) && stateRankedBlocks.length > 0) {
      setMemberRankedBlocks(stateRankedBlocks);
      localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, 'true');
      return;
    }

    let isMounted = true;
    const loadMemberResults = async () => {
      setIsLoadingMemberResults(true);
      try {
        const response = await fetch('http://localhost:8080/api/profile/results', {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`
          }
        });

        const responseBody = await response.json().catch(() => ({}));
        const latestResults = Array.isArray(responseBody?.results) ? responseBody.results : [];

        if (!isMounted) return;
        setMemberRankedBlocks(latestResults);

        localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, latestResults.length > 0 ? 'true' : 'false');
      } catch (error) {
        if (!isMounted) return;
        setMemberRankedBlocks([]);
        localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, 'false');
      } finally {
        if (isMounted) {
          setIsLoadingMemberResults(false);
        }
      }
    };

    loadMemberResults();
    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, token, stateRankedBlocks]);

  const rankedBlocks = useMemo(() => {
    if (isAuthenticated) {
      // Priority for logged-in users: freshly generated results -> DB results.
      if (Array.isArray(stateRankedBlocks) && stateRankedBlocks.length > 0) {
        return stateRankedBlocks;
      }
      return memberRankedBlocks;
    }
    return stateRankedBlocks ?? cachedRankedBlocks;
  }, [isAuthenticated, memberRankedBlocks, stateRankedBlocks, cachedRankedBlocks]);

  if (stateRankedBlocks !== null) {
    if (!isAuthenticated && stateRankedBlocks.length > 0) {
      sessionStorage.setItem(TEMP_RESULTS_KEY, JSON.stringify(stateRankedBlocks));
    } else if (!isAuthenticated) {
      sessionStorage.removeItem(TEMP_RESULTS_KEY);
    }

    if (isAuthenticated) {
      localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, stateRankedBlocks.length > 0 ? 'true' : 'false');
    }
  }

  if (isAuthenticated && isLoadingMemberResults) {
    return (
      <div className="no-results-container">
        <h2>Loading Your Saved Results...</h2>
        <p>Retrieving your latest personalized recommendations from your profile.</p>
      </div>
    );
  }

  if (rankedBlocks.length === 0) {
    if (!isAuthenticated) {
      return (
        <div className="no-results-container">
          <div className="no-results-hero-card">
            <SearchX className="no-results-icon" size={42} strokeWidth={2} />
            <h2>Please Log In or Sign Up to see your personalized results.</h2>
            <p>Create an account or log in to unlock your tailored HDB recommendations.</p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginTop: '16px' }}>
            <button onClick={() => navigate('/login')} className="back-btn">Log In</button>
            <button onClick={() => navigate('/create-account')} className="back-btn">Sign Up</button>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="no-results-container">
        <div className="no-results-hero-card">
          <SearchX className="no-results-icon" size={42} strokeWidth={2} />
          <h2>No Matches Found</h2>
          <p>We couldn't find any HDBs matching your specific criteria. Try broadening your budget or location!</p>
          <button onClick={() => navigate('/quiz')} className="back-btn">Back to Quiz</button>
        </div>
      </div>
    );
  }

  return (
    <div className="results-page-wrapper">
      {!isAuthenticated && (
        <div className="guest-results-banner">
          Viewing temporary results.{' '}
          <Link to="/signup" className="guest-results-signup-link">
            Sign up to save these to your profile permanently!
          </Link>
        </div>
      )}

      <div className="results-header">
        <h1>Your HDB Matches</h1>
        <p>We found {rankedBlocks.length} blocks tailored to your lifestyle preferences.</p>
      </div>

      <div className="results-grid">
        {rankedBlocks.map((item, index) => {
          const block = item?.hdbBlock ?? item ?? {};
          const blockId = block?.blockId ?? 'N/A';
          const town = block?.town ?? 'Unknown Town';
          const postalCode = block?.postalCode ?? 'N/A';

          const globalMatchIndex = getSafeNumber(item?.globalMatchIndex ?? block?.globalMatchIndex);
          const estimatedPrice = getSafeNumber(item?.estimatedPrice ?? block?.estimatedPrice);
          const remainingLeaseYears = getSafeNumber(block?.remainingLeaseYears);

          const commuteFairnessScore = getSafeNumber(item?.commuteMetrics?.commuteFairnessScore);
          const totalCommuteBurden = getSafeNumber(item?.commuteMetrics?.totalCommuteBurden);
          const fairnessWidth = commuteFairnessScore === null
            ? 0
            : Math.max(0, Math.min(100, commuteFairnessScore * 100));

          const isHighMatch = globalMatchIndex !== null && globalMatchIndex > 75;
          const noiseRiskLevel = block?.noiseRiskLevel ?? '';

          return (
            <div key={`${blockId}-${index}`} className="hdb-card">
              <div className={`match-badge ${isHighMatch ? 'high' : 'med'}`}>
                {formatMatchScore(globalMatchIndex)}
              </div>

              <div className="card-content">
                <h3>{town}</h3>
                <p className="postal-code">Block ID: {blockId} | Postal: {postalCode}</p>

                <hr />

                <div className="details-row">
                  <div className="detail-item">
                    <span>Estimated Price</span>
                    <strong>{formatCurrency(estimatedPrice)}</strong>
                  </div>
                  <div className="detail-item">
                    <span>Lease Remaining</span>
                    <strong>{formatLeaseYears(remainingLeaseYears)}</strong>
                  </div>
                </div>

                {commuteFairnessScore !== null && (
                  <div className="commute-section">
                    <div className="fairness-bar-container">
                      <span className="label">Commute Fairness</span>
                      <div className="fairness-track">
                        <div
                          className="fairness-fill"
                          style={{ width: `${fairnessWidth}%` }}
                        ></div>
                      </div>
                    </div>
                    <p className="commute-time">
                      Total Daily Burden: {totalCommuteBurden ?? 'N/A'} mins
                    </p>
                  </div>
                )}

                <div className="tags-container">
                  {block?.westSunStatus && <span className="tag sun">Afternoon Sun</span>}
                  {String(noiseRiskLevel).toLowerCase() === 'high' && <span className="tag noise">High Noise Risk</span>}
                  {block?.futureRiskFlag && <span className="tag risk">Construction Risk</span>}
                </div>
              </div>

              <button className="view-details-btn">View on Map</button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default ResultsPage;
