import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import "../styles/QuizPage.css";
import StructuralConstraints from "../components/StructuralConstraints";
import LivabilityFactors from "../components/LivabilityFactors";
import CommuterAnalysis from "../components/CommuterAnalysis";
import QuizSummary from "../components/QuizSummary";
import { REGION_TOWN_MAP } from "../components/StructuralConstraints"; //

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

    // Create the final payload mapped to UserProfile.java
    const payload = {
      ...formData,
      structuralConstraints: {
        ...formData.structuralConstraints,
        preferredTowns: Array.from(finalTowns) // Overwrite with the auto-filled array
      }
    };

    const response = await fetch("http://localhost:8080/api/hdb/recommend", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok) throw new Error("Failed to submit quiz data");

    const result = await response.json();
    navigate("/results", { state: { rankedBlocks: result } });
  } catch (error) {
    console.error("Error submitting quiz:", error);
    alert("No matches found or system error. Please check your criteria.");
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
    formData.structuralConstraints.minLeaseYears >= 50;

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