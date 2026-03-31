import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import "../styles/QuizPage.css";
import StructuralConstraints from "../components/StructuralConstraints";
import LivabilityFactors from "../components/LivabilityFactors";
import CommuterAnalysis from "../components/CommuterAnalysis";
import QuizSummary from "../components/QuizSummary";
import { REGION_TOWN_MAP } from "../components/StructuralConstraints"; //

const RESULTS_CACHE_KEY = "latestRankedBlocks";
const MEMBER_RESULTS_AVAILABLE_KEY = "memberResultsAvailable";

const PREFERENCE_NAME_MAP = {
  solarOrientation: "Solar Orientation",
  acousticComfort: "Acoustic Comfort",
  convenience: "Convenience"
};

const parseCoordinateInput = (value) => {
  if (typeof value !== "string") return null;
  const [latText, lngText] = value.split(",").map((part) => part.trim());
  const lat = Number(latText);
  const lng = Number(lngText);

  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  return { lat, lng };
};

const buildBackendPayload = (formData, preferredTowns) => {
  const [minBudget = 0, maxBudget = 0] = formData.structuralConstraints.budgetRange || [];

  return {
    userId: formData.userId,
    structuralConstraints: {
      maxBudget: Math.max(minBudget, maxBudget),
      preferredTowns,
      preferredFlatType: formData.structuralConstraints.preferredFlatType,
      minLeaseYears: formData.structuralConstraints.minLeaseYears
    },
    commuterProfile: {
      isEnabled: formData.commuterProfile.enabled,
      destinationA: parseCoordinateInput(formData.commuterProfile.destinations[0]),
      destinationB: parseCoordinateInput(formData.commuterProfile.destinations[1])
    },
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

function QuizPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [attemptedNext, setAttemptedNext] = useState(false);
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    userId: 0,
    structuralConstraints: {
      budgetRange: [450000, 650000], 
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
  });

const submitQuiz = async () => {
  try {
    let finalTowns = new Set(formData.structuralConstraints.preferredTowns);
    
    formData.structuralConstraints.preferredRegions.forEach(region => {
      const regionTowns = REGION_TOWN_MAP[region];
      const hasTownsSelected = regionTowns.some(town => finalTowns.has(town));
      
      if (!hasTownsSelected) {
        regionTowns.forEach(town => finalTowns.add(town));
      }
    });

    const payload = buildBackendPayload(formData, Array.from(finalTowns));

    const response = await fetch("http://localhost:8080/api/hdb/recommend", {
      method: "POST",
      headers: buildAuthHeaders(),
      body: JSON.stringify(payload)
    });

    const responseBody = await parseBackendResponse(response);
    if (!response.ok) {
      if (response.status === 400) {
        alert(extractErrorMessage(responseBody, "Some quiz inputs are invalid. Please review your criteria and try again."));
        return;
      }

      if (response.status === 404) {
        alert(extractErrorMessage(responseBody, "No matches found. Please broaden your search criteria."));
        return;
      }

      if (response.status === 401) {
        alert(extractErrorMessage(responseBody, "Please log in or sign up to see your personalized results."));
        navigate("/login");
        return;
      }

      if (response.status >= 500) {
        alert(extractErrorMessage(responseBody, "A server error occurred. Please try again later."));
        return;
      }

      throw new Error(extractErrorMessage(responseBody, "Failed to submit quiz data"));
    }

    const rankedBlocks = extractRankedBlocks(responseBody);
    const token = localStorage.getItem("token") || "";
    const user = localStorage.getItem("user");
    const isAuthenticated = Boolean(token && user);

    if (!isAuthenticated && rankedBlocks.length > 0) {
      localStorage.setItem(RESULTS_CACHE_KEY, JSON.stringify(rankedBlocks));
      localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "false");
    } else if (!isAuthenticated) {
      localStorage.removeItem(RESULTS_CACHE_KEY);
      localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, "false");
    } else {
      // Members persist results in backend DB; keep only a lightweight availability flag on UI.
      localStorage.setItem(MEMBER_RESULTS_AVAILABLE_KEY, rankedBlocks.length > 0 ? "true" : "false");
    }

    navigate("/results", { state: { rankedBlocks } });
  } catch (error) {
    console.error("Error submitting quiz:", error);
    alert(error?.message || "Unable to submit your quiz right now. Please try again.");
  }
};

  const step = parseInt(searchParams.get("step")) || 1;

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

  const isStep2Valid = formData.softConstraints.every(factor => {
    if (factor.mode === "ignore" || factor.mode === "strict") return true;
    if (factor.mode === "weighted") return factor.weight >= 0;
    return false;
  });


  const isStep3Valid =
    !formData.commuterProfile.enabled ||
    (
      formData.commuterProfile.destinations[0].trim() !== "" &&
      formData.commuterProfile.destinations[1].trim() !== ""
    );

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
          <button onClick={step === 4 ? submitQuiz : nextStep} className="btn-next">
            {step === 4 ? "Find My HDB Match" : "Next >"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default QuizPage;