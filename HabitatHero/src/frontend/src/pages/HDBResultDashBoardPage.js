import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { SearchX, ArrowRight } from 'lucide-react';
import '../styles/HDBResultDashBoardPage.css';

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
  return years === null ? 'N/A' : `${years} years`;
};

function HDBResultDashBoardPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const token = localStorage.getItem('token') || '';
  const user = localStorage.getItem('user');
  const isAuthenticated = Boolean(token && user);

  const [memberRankedBlocks, setMemberRankedBlocks] = useState([]);
  const [isLoadingMemberResults, setIsLoadingMemberResults] = useState(false);
  const [hasQuizBeenTaken, setHasQuizBeenTaken] = useState(false);

  const cachedRankedBlocks = (() => {
    try {
      const parsed = JSON.parse(sessionStorage.getItem(TEMP_RESULTS_KEY) || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return [];
    }
  })();

  const formatTown = (town) => {
    if (!town) return "Unknown Town";

    return town
      .toLowerCase()
      .replace(/(^|\s|\/)\S/g, (char) => char.toUpperCase());
  };

  const stateRankedBlocks = Array.isArray(location.state?.rankedBlocks)
    ? location.state.rankedBlocks
    : null;

  useEffect(() => {
    if (cachedRankedBlocks.length > 0) {
      setHasQuizBeenTaken(true);
    }

    // Guests: no backend fetch needed.
    if (!isAuthenticated) return;

    // If we just navigated from quiz with fresh results, show them immediately.
    if (Array.isArray(stateRankedBlocks) && stateRankedBlocks.length > 0) {
      setMemberRankedBlocks(stateRankedBlocks);
      setHasQuizBeenTaken(true);
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
        
        // Mark quiz as taken if there are results from backend
        if (latestResults.length > 0) {
          setHasQuizBeenTaken(true);
        }

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
  }, [cachedRankedBlocks.length, isAuthenticated, token, stateRankedBlocks]);

  const rankedBlocks = useMemo(() => {
    if (isAuthenticated) {
      // Priority for logged-in users: freshly generated results -> DB results.
      if (Array.isArray(stateRankedBlocks) && stateRankedBlocks.length > 0) {
        return stateRankedBlocks;
      }
      if (memberRankedBlocks.length > 0) {
        return memberRankedBlocks;
      }
      return cachedRankedBlocks;
    }
    return stateRankedBlocks ?? cachedRankedBlocks;
  }, [isAuthenticated, memberRankedBlocks, stateRankedBlocks, cachedRankedBlocks]);

  const hasFreshNavigationResults =
    Array.isArray(stateRankedBlocks) && stateRankedBlocks.length > 0;
  const usingSessionFallback =
    isAuthenticated &&
    rankedBlocks.length > 0 &&
    cachedRankedBlocks.length > 0 &&
    memberRankedBlocks.length === 0;
  const hasProfileSavedResults =
    isAuthenticated &&
    rankedBlocks.length > 0 &&
    (memberRankedBlocks.length > 0 || hasFreshNavigationResults) &&
    !usingSessionFallback;

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

    // Check if quiz has never been taken
    if (!hasQuizBeenTaken) {
      return (
        <div className="no-results-container">
          <div className="no-results-hero-card">
            <SearchX className="no-results-icon" size={42} strokeWidth={2} />
            <h2>Start the quiz now!</h2>
            <p>Take our personalized quiz to discover HDB flats that match your lifestyle and preferences.</p>
            <button onClick={() => navigate('/quiz')} className="back-btn">Start Quiz</button>
          </div>
        </div>
      );
    }

    // Quiz was taken but no matches found
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
    <div className="results-page">
      {!isAuthenticated && (
        <div className="guest-results-banner">
          Viewing temporary results.{' '}
          <Link to="/signup" className="guest-results-signup-link">
            Sign up to save these to your profile permanently!
          </Link>
        </div>
      )}

      {hasProfileSavedResults && (
        <div className="result-status-banner result-status-banner--success">
          Saved to your profile. Your recommendations will still be here when you come back.
        </div>
      )}

      {usingSessionFallback && (
        <div className="result-status-banner result-status-banner--warning">
          Showing temporary session results. Your login may have expired, so these might not be saved to your profile yet.
        </div>
      )}

      <div className="results-header">
        <h1>Your Personalized HDB Matches</h1>
        <p>We found {rankedBlocks.length} blocks tailored to your lifestyle preferences.</p>
      </div>

      <div className="results-layout">
      <section className="results-panel">
        <div className="results-grid">
          {rankedBlocks.map((item, index) => {
            const block = item?.hdbBlock ?? item ?? {};
            const blockId = block?.blockId ?? "N/A";
            const town = formatTown(block?.town);
            const postalCode = block?.postalCode ?? "N/A";
            const blockNumber = block?.blockNumber ?? "N/A";
            const streetName = formatTown(block?.streetName);

            const globalMatchIndex = getSafeNumber(
              item?.globalMatchIndex ?? block?.globalMatchIndex
            );
            const estimatedPrice = getSafeNumber(
              item?.estimatedPrice ?? block?.estimatedPrice
            );
            const remainingLeaseYears = getSafeNumber(block?.remainingLeaseYears);

            const commuteFairnessScore = getSafeNumber(
              item?.commuteMetrics?.commuteFairnessScore
            );
            const totalCommuteBurden = getSafeNumber(
              item?.commuteMetrics?.totalCommuteBurden
            );

            const fairnessWidth =
              commuteFairnessScore === null
                ? 0
                : Math.max(0, Math.min(100, commuteFairnessScore * 100));

            const isHighMatch =
              globalMatchIndex !== null && globalMatchIndex > 75;

            const noiseRiskLevel = block?.noiseRiskLevel ?? "";

            return (
              <div key={`${blockId}-${index}`} className="result-card">
                <div className="result-header-row">
                  <div>
                    <h3 className="result-title">Block {blockNumber} {streetName}</h3>
                    <p className="result-postal">
                      Town: {town} | Postal: {postalCode}
                    </p>
                  </div>

                  <div className="result-side">
                    <div
                      className={`match-badge ${
                        isHighMatch ? "high" : "med"
                      }`}
                    >
                      {formatMatchScore(globalMatchIndex)}
                    </div>
                    <Link to={`/result-detail/blkid-${blockId}`} state={{ block }} className='details-link'>
                    <button className="view-details-btn">
                      View Details
                      <ArrowRight size={14} />
                    </button>
                  </Link>
                  </div>
                </div>

                <hr />

                <div className="result-details-row">
                  <div className="detail-item">
                    <span>Estimated Price: </span>
                    <strong>{formatCurrency(estimatedPrice)}</strong>
                  </div>

                  <div className="detail-item">
                    <span>Lease Remaining: </span>
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
                        />
                      </div>
                    </div>

                    <p className="commute-time">
                      Total Daily Burden:{" "}
                      {totalCommuteBurden ?? "N/A"} mins
                    </p>
                  </div>
                )}

                <div className="tags-container">
                  {block?.westSunStatus && (
                    <span className="tag sun">Afternoon Sun</span>
                  )}

                  {String(noiseRiskLevel).toLowerCase() === "high" && (
                    <span className="tag noise">High Noise Risk</span>
                  )}

                  {block?.futureRiskFlag && (
                    <span className="tag risk">Construction Risk</span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
      
    </div>
  );
}

export default HDBResultDashBoardPage;
