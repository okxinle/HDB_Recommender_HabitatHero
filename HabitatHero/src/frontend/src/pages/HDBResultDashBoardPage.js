import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { SearchX, ArrowRight, Search } from 'lucide-react';
import '../styles/HDBResultDashBoardPage.css';

const TEMP_RESULTS_KEY = 'temporaryGuestResults';
const MEMBER_RESULTS_AVAILABLE_KEY = 'memberResultsAvailable';
const RESULTS_PREFERENCES_KEY = 'resultsSubmittedPreferences';
const LOADING_UI_VARIANT = 'skeleton';
const SKELETON_CARD_COUNT = 3;

const AMENITY_LABEL_MAP = {
  school: 'School',
  hawkerCentre: 'Hawker Centre',
  supermarket: 'Supermarket',
  park: 'Park',
  hospital: 'Hospital',
  playground: 'Playground',
  parentsAddress: "Parents' Home"
};

const PREFERENCE_LABEL_MAP = {
  solarOrientation: 'Solar Orientation',
  acousticComfort: 'Acoustic Comfort',
  convenience: 'Convenience'
};

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

const formatConvenienceMatch = (value) => {
  const score = getSafeNumber(value);
  return score === null ? 'N/A' : `${Math.round(score * 100)}%`;
};

const getFactorMode = (submittedPreferences, factorName) => {
  const softConstraints = Array.isArray(submittedPreferences?.softConstraints)
    ? submittedPreferences.softConstraints
    : [];
  const factor = softConstraints.find((item) => item?.preferenceName === factorName);
  return factor?.mode || 'ignore';
};

const skeletonCardIndices = Array.from({ length: SKELETON_CARD_COUNT }, (_, index) => index);

function ResultsSkeletonCard() {
  return (
    <div className="result-card result-card--skeleton" aria-hidden="true">
      <div className="result-header-row">
        <div className="skeleton-col">
          <div className="skeleton-line skeleton-line--title" />
          <div className="skeleton-line skeleton-line--subtitle" />
        </div>
        <div className="skeleton-side">
          <div className="skeleton-pill" />
          <div className="skeleton-button" />
        </div>
      </div>

      <hr />

      <div className="result-details-row">
        <div className="detail-item detail-item--skeleton">
          <div className="skeleton-line skeleton-line--detail-label" />
          <div className="skeleton-line skeleton-line--detail-value" />
        </div>

        <div className="detail-item detail-item--skeleton">
          <div className="skeleton-line skeleton-line--detail-label" />
          <div className="skeleton-line skeleton-line--detail-value" />
        </div>

        <div className="detail-item detail-item--skeleton">
          <div className="skeleton-line skeleton-line--detail-label" />
          <div className="skeleton-line skeleton-line--detail-value" />
        </div>
      </div>

      <div className="skeleton-tags-row">
        <span className="skeleton-tag" />
        <span className="skeleton-tag" />
      </div>
    </div>
  );
}

function ResultsSpinnerLoader() {
  return (
    <div className="results-loading-spinner" role="status" aria-live="polite" aria-label="Loading saved results">
      <div className="results-loading-spinner__ring" />
      <p>Loading your saved results...</p>
    </div>
  );
}

function HDBResultDashBoardPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const token = localStorage.getItem('token') || '';
  const user = localStorage.getItem('user');
  const isAuthenticated = Boolean(token && user);

  const [memberRankedBlocks, setMemberRankedBlocks] = useState([]);
  const [isLoadingMemberResults, setIsLoadingMemberResults] = useState(false);
  const [hasQuizBeenTaken, setHasQuizBeenTaken] = useState(false);
  const [showResultsFadeIn, setShowResultsFadeIn] = useState(false);

  const [searchQuery, setSearchQuery] = useState(() => {
    return sessionStorage.getItem('resultsSearchQuery') || '';
  });
  const [sortBy, setSortBy] = useState(() => {
    return sessionStorage.getItem('resultsSortBy') || 'match-desc';
  });

  useEffect(() => {
    sessionStorage.setItem('resultsSearchQuery', searchQuery);
  }, [searchQuery]);

  useEffect(() => {
    sessionStorage.setItem('resultsSortBy', sortBy);
  }, [sortBy]);

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
  const stateSubmittedPreferences = location.state?.submittedPreferences ?? null;

  const storedSubmittedPreferences = (() => {
    try {
      const parsed = JSON.parse(localStorage.getItem(RESULTS_PREFERENCES_KEY) || 'null');
      return parsed && typeof parsed === 'object' ? parsed : null;
    } catch (error) {
      return null;
    }
  })();

  useEffect(() => {
    if (stateSubmittedPreferences) {
      localStorage.setItem(RESULTS_PREFERENCES_KEY, JSON.stringify(stateSubmittedPreferences));
    }

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
  }, [cachedRankedBlocks.length, isAuthenticated, token, stateRankedBlocks, stateSubmittedPreferences]);

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

  const submittedPreferences = stateSubmittedPreferences ?? storedSubmittedPreferences;

  useEffect(() => {
    if (!isLoadingMemberResults && rankedBlocks.length > 0) {
      const animationTimer = window.setTimeout(() => {
        setShowResultsFadeIn(true);
      }, 20);
      return () => window.clearTimeout(animationTimer);
    }
    setShowResultsFadeIn(false);
    return undefined;
  }, [isLoadingMemberResults, rankedBlocks.length]);

  const filteredAndSortedResults = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();

    const filteredResults = rankedBlocks.filter((item) => {
      const block = item?.hdbBlock ?? item ?? {};
      const estimatedPrice = getSafeNumber(item?.estimatedPrice ?? block?.estimatedPrice);
      const remainingLeaseYears = getSafeNumber(block?.remainingLeaseYears);

      const searchableFields = [
        block?.blockNumber,
        block?.streetName,
        block?.postalCode,
        block?.town,
        estimatedPrice,
        estimatedPrice !== null ? estimatedPrice.toLocaleString() : null,
        remainingLeaseYears
      ];

      const searchableText = searchableFields
        .filter((value) => value !== null && value !== undefined)
        .map((value) => String(value).toLowerCase())
        .join(' ');

      if (!normalizedQuery) {
        return true;
      }

      return searchableText.includes(normalizedQuery);
    });

    return [...filteredResults].sort((a, b) => {
      const blockA = a?.hdbBlock ?? a ?? {};
      const blockB = b?.hdbBlock ?? b ?? {};

      const matchA = getSafeNumber(a?.globalMatchIndex ?? blockA?.globalMatchIndex);
      const matchB = getSafeNumber(b?.globalMatchIndex ?? blockB?.globalMatchIndex);
      const priceA = getSafeNumber(a?.estimatedPrice ?? blockA?.estimatedPrice);
      const priceB = getSafeNumber(b?.estimatedPrice ?? blockB?.estimatedPrice);
      const floorAreaA = getSafeNumber(a?.floorAreaSqm ?? blockA?.floorAreaSqm);
      const floorAreaB = getSafeNumber(b?.floorAreaSqm ?? blockB?.floorAreaSqm);
      const leaseA = getSafeNumber(blockA?.remainingLeaseYears);
      const leaseB = getSafeNumber(blockB?.remainingLeaseYears);

      switch (sortBy) {
        case 'price-asc':
          return (priceA ?? Number.POSITIVE_INFINITY) - (priceB ?? Number.POSITIVE_INFINITY);
        case 'price-desc':
          return (priceB ?? Number.NEGATIVE_INFINITY) - (priceA ?? Number.NEGATIVE_INFINITY);
        case 'area-desc':
          return (floorAreaB ?? Number.NEGATIVE_INFINITY) - (floorAreaA ?? Number.NEGATIVE_INFINITY);
        case 'area-asc':
          return (floorAreaA ?? Number.POSITIVE_INFINITY) - (floorAreaB ?? Number.POSITIVE_INFINITY);
        case 'lease-desc':
          return (leaseB ?? Number.NEGATIVE_INFINITY) - (leaseA ?? Number.NEGATIVE_INFINITY);
        case 'match-desc':
        default:
          return (matchB ?? Number.NEGATIVE_INFINITY) - (matchA ?? Number.NEGATIVE_INFINITY);
      }
    });
  }, [rankedBlocks, searchQuery, sortBy]);

  const summaryData = useMemo(() => {
    if (!submittedPreferences) {
      return null;
    }

    const structural = submittedPreferences.structuralConstraints || {};
    const commuter = submittedPreferences.commuterProfile || {};
    const softConstraints = Array.isArray(submittedPreferences.softConstraints)
      ? submittedPreferences.softConstraints
      : [];
    const convenienceConfig = softConstraints.find((item) => item?.preferenceName === 'convenience') || {};

    const budgetRange = Array.isArray(structural.budgetRange) ? structural.budgetRange : [];
    const minBudget = getSafeNumber(budgetRange[0]);
    const maxBudget = getSafeNumber(budgetRange[1]);

    const regions = Array.isArray(structural.preferredRegions)
      ? structural.preferredRegions.filter(Boolean)
      : [];
    const towns = Array.isArray(structural.preferredTowns)
      ? structural.preferredTowns.filter(Boolean)
      : [];

    const destinations = Array.isArray(commuter.destinations)
      ? commuter.destinations.filter((value) => typeof value === 'string' && value.trim().length > 0)
      : [];

    const amenities = Array.isArray(convenienceConfig.selectedAmenities)
      ? convenienceConfig.selectedAmenities
          .filter(Boolean)
          .map((key) => AMENITY_LABEL_MAP[key] || key)
      : [];

    const factorModes = softConstraints.map((factor) => {
      const mode = factor?.mode || 'ignore';
      const weight = getSafeNumber(factor?.weight);
      const factorLabel = PREFERENCE_LABEL_MAP[factor?.preferenceName] || factor?.preferenceName || 'Unknown';
      if (mode === 'weighted' && weight !== null) {
        return `${factorLabel}: Weighted (${weight})`;
      }
      if (mode === 'strict') {
        return `${factorLabel}: Strict`;
      }
      return `${factorLabel}: Ignore`;
    });

    return {
      budgetText:
        minBudget !== null && maxBudget !== null
          ? `$${minBudget.toLocaleString()} - $${maxBudget.toLocaleString()}`
          : 'Not set',
      flatType: structural.preferredFlatType || 'Not set',
      minLeaseYears:
        Number.isFinite(structural.minLeaseYears)
          ? `${structural.minLeaseYears} years`
          : 'Not set',
      regionsText: regions.length > 0 ? regions.join(', ') : 'Not set',
      townsText: towns.length > 0 ? towns.join(', ') : 'Not set',
      commuterEnabled: Boolean(commuter.enabled),
      commuterDestinations: destinations,
      convenienceMode: convenienceConfig.mode || 'ignore',
      amenities,
      factorModes
    };
  }, [submittedPreferences]);

  const cardVisibilityConfig = useMemo(() => {
    return {
      solarMode: getFactorMode(submittedPreferences, 'solarOrientation'),
      acousticMode: getFactorMode(submittedPreferences, 'acousticComfort'),
      convenienceMode: getFactorMode(submittedPreferences, 'convenience'),
      commuterEnabled: Boolean(submittedPreferences?.commuterProfile?.enabled)
    };
  }, [submittedPreferences]);

  const handleEditPreferences = () => {
    navigate('/quiz?step=1');
  };

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
    const isSkeletonMode = LOADING_UI_VARIANT === 'skeleton';

    if (isSkeletonMode) {
      return (
        <div className="results-page">
          <div className="results-loading-header">
            <div className="results-loading-chip">Syncing your profile data</div>
            <h1>Preparing your personalized matches</h1>
            <p>Just a moment while we pull your latest saved recommendations.</p>
          </div>

          <section className="results-panel">
            <div className="results-grid results-grid--loading" aria-live="polite" aria-busy="true">
              {skeletonCardIndices.map((index) => (
                <ResultsSkeletonCard key={`skeleton-card-${index}`} />
              ))}
            </div>
          </section>
        </div>
      );
    }

    return (
      <div className="no-results-container">
        <ResultsSpinnerLoader />
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

      <section className="choice-summary-card">
        <div className="choice-summary-header">
          <h2>Your Preferences Summary</h2>
          <button type="button" className="edit-preferences-btn" onClick={handleEditPreferences}>
            Edit Preferences
          </button>
        </div>

        {!summaryData ? (
          <p className="choice-summary-empty">No quiz summary found yet. Take the quiz to build your preferences.</p>
        ) : (
          <div className="choice-summary-grid">
            <div className="choice-summary-item"><strong>Budget:</strong> {summaryData.budgetText}</div>
            <div className="choice-summary-item"><strong>Flat Type:</strong> {summaryData.flatType}</div>
            <div className="choice-summary-item"><strong>Minimum Lease:</strong> {summaryData.minLeaseYears}</div>
            <div className="choice-summary-item"><strong>Preferred Regions:</strong> {summaryData.regionsText}</div>
            <div className="choice-summary-item"><strong>Preferred Towns:</strong> {summaryData.townsText}</div>
            <div className="choice-summary-item">
              <strong>Multi-Commuter Analysis:</strong> {summaryData.commuterEnabled ? 'Enabled' : 'Disabled'}
              {summaryData.commuterEnabled && summaryData.commuterDestinations.length > 0 && (
                <span> ({summaryData.commuterDestinations.join(' & ')})</span>
              )}
            </div>
            <div className="choice-summary-item"><strong>Convenience Mode:</strong> {summaryData.convenienceMode}</div>
            <div className="choice-summary-item">
              <strong>Selected Amenities:</strong> {summaryData.amenities.length > 0 ? summaryData.amenities.join(', ') : 'None'}
            </div>
            <div className="choice-summary-item choice-summary-item--full">
              <strong>Factor Preferences:</strong> {summaryData.factorModes.length > 0 ? summaryData.factorModes.join(' | ') : 'None'}
            </div>
          </div>
        )}
      </section>

      <div className="results-header">
        <h1>Your Personalized HDB Matches</h1>
        <p>We found {filteredAndSortedResults.length} blocks tailored to your lifestyle preferences.</p>

        <div className="results-topbar flex flex-col md:flex-row justify-center items-center gap-4 w-full max-w-5xl mx-auto mb-10" role="region" aria-label="Search and sort results">
          <div className="results-topbar-search-wrap w-full md:w-[66%]">
            <Search size={16} className="results-topbar-search-icon" aria-hidden="true" />
            <input
              type="text"
              className="results-topbar-input rounded-full border border-gray-300 shadow-sm h-12 px-10 w-full focus:ring-0 focus:outline-none"
              placeholder="Search by street name..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              aria-label="Global search"
            />
          </div>

          <select
            className="results-topbar-select rounded-full border border-gray-300 shadow-sm h-12 px-6 w-full md:w-[33%] bg-white"
            value={sortBy}
            onChange={(event) => setSortBy(event.target.value)}
            aria-label="Sort results"
          >
            <option value="match-desc">Highest Match % (Default)</option>
            <option value="price-asc">Price: Low to High</option>
            <option value="price-desc">Price: High to Low</option>
            <option value="area-desc">Floor Area: Large to Small</option>
            <option value="area-asc">Floor Area: Small to Large</option>
            <option value="lease-desc">Lease: Newest First (Longest lease remaining)</option>
          </select>
        </div>
      </div>

      <div className="results-layout">
      <section className="results-panel">
        {filteredAndSortedResults.length === 0 ? (
          <div className="results-empty-filter-state">
            <SearchX size={18} strokeWidth={2} aria-hidden="true" />
            <span>No matches found</span>
          </div>
        ) : (
        <div className={`results-grid ${showResultsFadeIn ? 'results-grid--fade-in' : ''}`}>
          {filteredAndSortedResults.map((item, index) => {
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
            const floorAreaSqm = getSafeNumber(
              item?.floorAreaSqm ?? block?.floorAreaSqm
            );
            const floorAreaSqft = floorAreaSqm === null
              ? null
              : floorAreaSqm * 10.7639;
            const remainingLeaseYears = getSafeNumber(block?.remainingLeaseYears);
            const convenienceScore = getSafeNumber(
              item?.convenienceScore ?? block?.convenienceScore
            );
            const convenienceFactors =
              item?.convenienceFactors ?? block?.convenienceFactors ?? {};
            const convenienceEntries = Object.entries(convenienceFactors);
            const hasConvenienceBreakdown = convenienceEntries.length > 0;
            const convenienceMatchText = hasConvenienceBreakdown
              ? formatConvenienceMatch(convenienceScore)
              : 'Unavailable';
            const convenienceMatchPercent = convenienceScore === null ? null : Math.round(convenienceScore * 100);

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
            const normalizedNoiseRisk = String(noiseRiskLevel).toLowerCase();
            const leaseIsAging = remainingLeaseYears !== null && remainingLeaseYears < 60;
            const convenienceIsStrong = convenienceMatchPercent !== null && convenienceMatchPercent > 80;

            const shouldShowNoiseInsight = cardVisibilityConfig.acousticMode !== 'ignore';
            const shouldShowSunInsight = cardVisibilityConfig.solarMode !== 'ignore';
            const shouldShowTransportInsight = cardVisibilityConfig.commuterEnabled;

            const hasNearbyMrt =
              convenienceFactors?.mrtStation ??
              convenienceFactors?.mrt ??
              convenienceFactors?.train ??
              false;
            const noiseClassName = normalizedNoiseRisk.includes('high')
              ? 'insight-value--danger'
              : normalizedNoiseRisk.includes('moderate') || normalizedNoiseRisk.includes('medium')
                ? 'insight-value--warning'
                : 'insight-value--success';

            return (
              <div key={`${blockId}-${index}`} className="result-card result-card--premium">
                <div className="result-header-row result-header-row--premium">
                  <div>
                    <h3 className="result-title result-title--premium">Block {blockNumber} {streetName}</h3>
                    <p className="result-postal result-postal--muted">
                      Town: {town} | Postal: {postalCode}
                    </p>
                  </div>

                  <div className="result-side result-side--premium">
                    <div className={`match-badge match-badge--premium ${isHighMatch ? 'high' : 'med'}`}>
                      {globalMatchIndex === null ? 'N/A' : `${globalMatchIndex.toFixed(1)}%`}
                    </div>
                  </div>
                </div>

                <hr className="result-divider" />

                <div className="result-stats-row">
                  <div className="result-stat-pill">
                    <span>💰</span>
                    <span>Estimated Price: <strong>{formatCurrency(estimatedPrice)}</strong></span>
                  </div>

                  <div className="result-stat-pill">
                    <span>📏</span>
                    <span>
                      Floor Area: <strong>{floorAreaSqm === null ? 'N/A' : `${Math.round(floorAreaSqft)} sqft`}</strong>
                    </span>
                  </div>

                  <div className="result-stat-pill">
                    <span>⏳</span>
                    <span>
                      Lease Remaining:{' '}
                      <strong className={leaseIsAging ? 'stat-value--warning' : ''}>{formatLeaseYears(remainingLeaseYears)}</strong>
                    </span>
                  </div>

                  <div className="result-stat-pill">
                    <span>🛒</span>
                    <span>
                      Convenience:{' '}
                      <strong className={convenienceIsStrong ? 'stat-value--success' : ''}>{convenienceMatchText}</strong>
                    </span>
                  </div>
                </div>

                <div className="result-insights-row">
                  {shouldShowNoiseInsight && (
                    <span className="insight-tag">
                      🔇 Noise Risk: <strong className={noiseClassName}>{noiseRiskLevel || 'Low'}</strong>
                    </span>
                  )}

                  {shouldShowSunInsight && (
                    <span className="insight-tag">
                      ☀️ West Sun: <strong className={block?.westSunStatus ? 'insight-value--warning' : 'insight-value--success'}>{block?.westSunStatus ? 'Moderate' : 'Low'}</strong>
                    </span>
                  )}

                  {shouldShowTransportInsight && (
                    <span className="insight-tag">
                      🚇 {hasNearbyMrt ? 'MRT nearby' : 'MRT not nearby'}
                    </span>
                  )}

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

                <div className="result-footer-actions">
                  <Link to={`/result-detail/blkid-${blockId}`} state={{ block, result: item }} className='details-link'>
                    <button className="view-details-btn view-details-btn--compact">
                      View Details
                      <ArrowRight size={14} />
                    </button>
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
        )}
      </section>
    </div>

    </div>
  );
}

export default HDBResultDashBoardPage;