import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import "../styles/QuizPage.css";
import StructuralConstraints, { REGION_TOWN_MAP } from "./QuizSteps/StructuralConstraints";
import LivabilityFactors from "./QuizSteps/LivabilityFactors";
import CommuterAnalysis from "./QuizSteps/CommuterAnalysis";
import QuizSummary from "./QuizSteps/QuizSummary";

const TEMP_RESULTS_KEY = "temporaryGuestResults";
const MEMBER_RESULTS_AVAILABLE_KEY = "memberResultsAvailable";
const QUIZ_DATA_KEY = "quizData";

const PREFERENCE_NAME_MAP = {
  solarOrientation: "Solar Orientation",
  acousticComfort: "Acoustic Comfort",
  convenience: "Convenience"
};

const isValidPostalCode = (value) => /^\d{6}$/.test((value || "").trim());

const getCurrentUserId = () => {
  try {
    const user = JSON.parse(localStorage.getItem("user") || "null");
    const userId = Number(user?.userId);
    return Number.isFinite(userId) ? userId : 0;
  } catch (error) {
    return 0;
  }
};

const buildBackendPayload = (formData, preferredTowns) => {
  const [minBudget = 0, maxBudget = 0] = formData.structuralConstraints.budgetRange || [];
  const postalCodeA = (formData.commuterProfile.destinations[0] || "").trim();
  const postalCodeB = (formData.commuterProfile.destinations[1] || "").trim();
  const convenienceConstraint = formData.softConstraints.find(
    (constraint) => constraint.preferenceName === "convenience"
  ) || { mode: "ignore", weight: 0, selectedAmenities: [], parentsAddress: "" };

  const convenienceMode = String(convenienceConstraint.mode || "ignore").toUpperCase();
  const convenienceWeight = convenienceMode === "WEIGHTED"
    ? Number(convenienceConstraint.weight || 0)
    : 0;
  const selectedAmenities = Array.isArray(convenienceConstraint.selectedAmenities)
    ? convenienceConstraint.selectedAmenities
    : [];
  const parentsPostalCode = (convenienceConstraint.parentsAddress || "").trim();

  return {
    userId: getCurrentUserId(),
    structuralConstraints: {
      maxBudget: Math.max(minBudget, maxBudget),
      preferredTowns,
      preferredFlatType: formData.structuralConstraints.preferredFlatType,
      minLeaseYears: formData.structuralConstraints.minLeaseYears
    },
    commuterProfile: {
      isEnabled: formData.commuterProfile.enabled,
      destinationA: null,
      destinationB: null
    },
    postalCodeA,
    postalCodeB,
    convenienceMode,
    convenienceWeight,
    selectedAmenities,
    parentsPostalCode,
    softConstraints: formData.softConstraints.map((constraint) => ({
      factorName: PREFERENCE_NAME_MAP[constraint.preferenceName] || constraint.preferenceName,
      priorityWeight: constraint.mode === "weighted" ? constraint.weight : 0,
      isStrict: constraint.mode === "strict"
    }))
  };
};

const parseBackendResponse = async (response) => {
  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  const text = await response.text();
  return text || null;
};

const extractRankedBlocks = (responseBody) => {
  if (Array.isArray(responseBody)) return responseBody;
  if (responseBody && Array.isArray(responseBody.results)) return responseBody.results;
  return [];
};

const extractErrorMessage = (responseBody, fallbackMessage) => {
  if (typeof responseBody === "string" && responseBody.trim().length > 0) {
    return responseBody;
  }

  if (responseBody && typeof responseBody.message === "string" && responseBody.message.trim().length > 0) {
    return responseBody.message;
  }

  if (responseBody && typeof responseBody.error === "string" && responseBody.error.trim().length > 0) {
    return responseBody.error;
  }

  return fallbackMessage;
};

const buildAuthHeaders = () => {
  const token = localStorage.getItem("token");
  const headers = { "Content-Type": "application/json" };

  if (token && token.trim().length > 0) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
};

const initialFormData = {
  userId: 0,
  structuralConstraints: {
    budgetRange: [400000, 800000],
    preferredRegions: [],
    preferredTowns: [],
    preferredFlatType: "",
    minLeaseYears: 50
  },
  commuterProfile: {
    enabled: false,
    destinations: ["", ""],
    fairnessWeight: 0.5
  },
  softConstraints: [
    { preferenceName: "solarOrientation", mode: "ignore", weight: 0 },
    { preferenceName: "acousticComfort", mode: "ignore", weight: 0 },
    { preferenceName: "convenience", mode: "ignore", weight: 0, selectedAmenities: [] }
  ]
};

function QuizPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [attemptedNext, setAttemptedNext] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const [formData, setFormData] = useState(initialFormData);

  const step = parseInt(searchParams.get("step")) || 1;

  useEffect(() => {
    const savedQuizData = sessionStorage.getItem(QUIZ_DATA_KEY);

    if (savedQuizData) {
      try {
        const parsed = JSON.parse(savedQuizData);
        setFormData(parsed);
      } catch (error) {
        console.warn("unable to parse saved quiz data:", error);
      }
    }
  }, []);

  useEffect(() => {
    sessionStorage.setItem(QUIZ_DATA_KEY, JSON.stringify(formData));
  }, [formData]);

  useEffect(() => {
    const stepFromUrl = parseInt(searchParams.get("step")) || 1;
    if (stepFromUrl < 1 || stepFromUrl > 4) {
      setSearchParams({ step: "1" });
    }
  }, [searchParams, setSearchParams]);

  const isStep1Valid =
    formData.structuralConstraints.budgetRange[0] < formData.structuralConstraints.budgetRange[1] &&
    formData.structuralConstraints.preferredRegions.length > 0 &&
    formData.structuralConstraints.preferredFlatType.length > 0 &&
    Number.isFinite(formData.structuralConstraints.minLeaseYears) &&
    formData.structuralConstraints.minLeaseYears >= 0;

  const isStep2Valid = formData.softConstraints.every((factor) => {
    if (factor.mode === "ignore" || factor.mode === "strict") return true;
    if (factor.mode === "weighted") return factor.weight >= 0;
    return false;
  });

  const isStep3Valid =
    !formData.commuterProfile.enabled ||
    (
      isValidPostalCode(formData.commuterProfile.destinations[0]) &&
      isValidPostalCode(formData.commuterProfile.destinations[1])
    );

  const isFinalSubmitDisabled = step === 4 && !isStep3Valid;

  const isCurrentStepValid = () => {
    if (step === 1) return isStep1Valid;
    if (step === 2) return isStep2Valid;
    if (step === 3) return isStep3Valid;
    return true;
  };

  const nextStep = () => {
    setAttemptedNext(true);
    if (!isCurrentStepValid()) return;
    setAttemptedNext(false);
    if (step < 4) setSearchParams({ step: String(step + 1) });
  };

  const prevStep = () => {
    setAttemptedNext(false);
    if (step > 1) setSearchParams({ step: String(step - 1) });
  };

  const submitQuiz = async () => {
    if (isLoading) return;

    setIsLoading(true);

    try {
      const finalTowns = new Set(formData.structuralConstraints.preferredTowns);

      formData.structuralConstraints.preferredRegions.forEach((region) => {
        const regionTowns = REGION_TOWN_MAP[region] || [];
        const hasTownsSelected = regionTowns.some((town) => finalTowns.has(town));

        if (!hasTownsSelected) {
          regionTowns.forEach((town) => finalTowns.add(town));
        }
      });

      const mergedPreferredTowns = Array.from(finalTowns);

      const finalFormData = {
        ...formData,
        structuralConstraints: {
          ...formData.structuralConstraints,
          preferredTowns: mergedPreferredTowns
        }
      };

      const payload = buildBackendPayload(finalFormData, mergedPreferredTowns);

      const response = await fetch("http://localhost:8080/api/hdb/recommend", {
        method: "POST",
        headers: buildAuthHeaders(),
        body: JSON.stringify(payload)
      });

      const responseBody = await parseBackendResponse(response);

      if (!response.ok) {
        if (response.status === 400) {
          alert(
            extractErrorMessage(
              responseBody,
              "some quiz inputs are invalid. please review your criteria and try again."
            )
          );
          return;
        }

        if (response.status === 404) {
          alert(
            extractErrorMessage(
              responseBody,
              "no matches found. please broaden your search criteria."
            )
          );
          return;
        }

        if (response.status === 401) {
          alert(
            extractErrorMessage(
              responseBody,
              "please log in or sign up to see your personalized results."
            )
          );
          navigate("/login");
          return;
        }

        if (response.status >= 500) {
          alert(
            extractErrorMessage(
              responseBody,
              "a server error occurred. please try again later."
            )
          );
          return;
        }

        throw new Error(
          extractErrorMessage(responseBody, "failed to submit quiz data")
        );
      }

      const rankedBlocks = extractRankedBlocks(responseBody);
      const token = localStorage.getItem("token") || "";
      const user = localStorage.getItem("user");
      const hasLocalAuth = Boolean(token && user);
      const backendPersisted = Boolean(responseBody?.resultsPersisted);

      sessionStorage.setItem(QUIZ_DATA_KEY, JSON.stringify(finalFormData));

      if (!hasLocalAuth && rankedBlocks.length > 0) {
        sessionStorage.setItem(TEMP_RESULTS_KEY, JSON.stringify(rankedBlocks));
        localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "false");
      } else if (!hasLocalAuth) {
        sessionStorage.removeItem(TEMP_RESULTS_KEY);
        localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "false");
      } else {
        let syncedToProfile = backendPersisted;

        if (rankedBlocks.length > 0) {
          try {
            const syncResponse = await fetch("http://localhost:8080/api/profile/results", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`
              },
              body: JSON.stringify(rankedBlocks)
            });

            if (syncResponse.ok) {
              syncedToProfile = true;
            }
          } catch (syncError) {
            console.warn("unable to sync quiz results to profile storage:", syncError);
          }
        }

        if (syncedToProfile) {
          sessionStorage.removeItem(TEMP_RESULTS_KEY);
          localStorage.setItem(
            MEMBER_RESULTS_AVAILABLE_KEY,
            rankedBlocks.length > 0 ? "true" : "false"
          );
        } else {
          // Keep a session fallback when auth token is stale or profile sync fails.
          if (rankedBlocks.length > 0) {
            sessionStorage.setItem(TEMP_RESULTS_KEY, JSON.stringify(rankedBlocks));
          } else {
            sessionStorage.removeItem(TEMP_RESULTS_KEY);
          }
          localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "false");
        }
      }

      navigate("/results", {
        state: {
          rankedBlocks,
          submittedPreferences: finalFormData
        }
      });
    } catch (error) {
      console.error("error submitting quiz:", error);
      alert(error?.message || "unable to submit your quiz right now. please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const renderStep = () => {
    const stepProps = { data: formData, update: setFormData, showErrors: attemptedNext };

    switch (step) {
      case 1: return <StructuralConstraints {...stepProps} />;
      case 2: return <LivabilityFactors {...stepProps} />;
      case 3: return <CommuterAnalysis {...stepProps} />;
      case 4: return <QuizSummary data={formData} />;
      default: return <StructuralConstraints {...stepProps} />;
    }
  };

  return (
    <div className="quiz-page-container">
      <div className="quiz-stepper">
        {["Structural Constraints", "Livability Factors", "Multi-Commuter Analysis", "Summary Of Preferences"].map((label, index) => {
          const num = index + 1;
          return (
            <div key={num} className={`step-wrapper ${num < 4 ? "has-line" : ""} ${step > num ? "completed" : ""}`}>
              <div className={`step-item ${step === num ? "active" : ""} ${step > num ? "completed" : ""}`}>
                <span className="step-number">{num}.</span>
                <span className="step-label">{label}</span>
              </div>
            </div>
          );
        })}
      </div>

      <div className="quiz-card-content">
        <div className="quiz-inner-content">{renderStep()}</div>
        <div className="quiz-actions">
          {step > 1 ? <button onClick={prevStep} className="btn-back">&lt; Back</button> : <div />}
          <button
            onClick={step === 4 ? submitQuiz : nextStep}
            className="btn-next"
            disabled={isFinalSubmitDisabled || (step === 4 && isLoading)}
          >
            {step === 4
              ? (isLoading ? "Analyzing blocks..." : "Find My HDB Match")
              : "Next >"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default QuizPage;