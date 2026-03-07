import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import "../styles/QuizPage.css";
import StructuralConstraints from "../components/StructuralConstraints";
import LivabilityFactors from "../components/LivabilityFactors";
import CommuterAnalysis from "../components/CommuterAnalysis";
import QuizSummary from "../components/QuizSummary";

function QuizPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [attemptedNext, setAttemptedNext] = useState(false);

  const [formData, setFormData] = useState({
    maxBudget: [400000, 450000],
    preferredTowns: [],
    preferredFlatType: "",
    minLeaseYears: 50,
    factors: {
      solarOrientation: { mode: null, weight: 0 },
      acousticComfort: { mode: null, weight: 0 },
      convenience: { mode: null, weight: 0 }
    },
    commuters: {
      enabled: false,
      destA: "",
      destB: "",
      fairness: 0.5
    }
  });

  const submitQuiz = async () => {
  try {
    const response = await fetch("http://localhost:8080/api/hdb/recommend", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(formData)
    });

    if (!response.ok) {
      throw new Error("Failed to submit quiz data");
    }

    const result = await response.json();
    console.log("Quiz submitted successfully:", result);
  } catch (error) {
    console.error("Error submitting quiz:", error);
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
    formData.maxBudget[0] < formData.maxBudget[1] &&
    formData.preferredTowns.length > 0 &&
    formData.preferredFlatType.length > 0 &&
    formData.minLeaseYears >= 50;

  const isFactorValid = (factor) => {
    if (!factor || !factor.mode) return false;

    if (factor.mode === "ignore") return true;
    if (factor.mode === "strict") return true;
    if (factor.mode === "weighted") {
      return factor.weight !== null && factor.weight !== undefined;
    }

    return false;
  };

  const isStep2Valid =
    isFactorValid(formData.factors.solarOrientation) &&
    isFactorValid(formData.factors.acousticComfort) &&
    isFactorValid(formData.factors.convenience);

  const isStep3Valid =
    !formData.commuters.enabled ||
    (
      formData.commuters.destA.trim() !== "" &&
      formData.commuters.destB.trim() !== "" &&
      formData.commuters.fairness !== null &&
      formData.commuters.fairness !== undefined
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

    if (step < 4) {
      setSearchParams({ step: String(step + 1) });
    }
  };

  const prevStep = () => {
    setAttemptedNext(false);

    if (step > 1) {
      setSearchParams({ step: String(step - 1) });
    }
  };

  const renderStep = () => {
    switch (step) {
      case 1:
        return (
          <StructuralConstraints
            data={formData}
            update={setFormData}
            showErrors={attemptedNext}
          />
        );
      case 2:
        return (
          <LivabilityFactors
            data={formData}
            update={setFormData}
            showErrors={attemptedNext}
          />
        );
      case 3:
        return (
          <CommuterAnalysis
            data={formData}
            update={setFormData}
            showErrors={attemptedNext}
          />
        );
      case 4:
        return <QuizSummary data={formData} />;
      default:
        return (
          <StructuralConstraints
            data={formData}
            update={setFormData}
            showErrors={attemptedNext}
          />
        );
    }
  };

  return (
    <div className="quiz-page-container">
      <div className="quiz-stepper">
        {[
          "Structural Constraints",
          "Livability Factors",
          "Multi-Commuter Analysis",
          "Summary Of Preferences",
        ].map((label, index) => {
          const num = index + 1;
          return (
            <div
              key={num}
              className={`step-wrapper ${num < 4 ? "has-line" : ""} ${
                step > num ? "completed" : ""
              }`}
            >
              <div
                className={`step-item ${
                  step === num ? "active" : ""
                } ${step > num ? "completed" : ""}`}
              >
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
          {step > 1 ? (
            <button onClick={prevStep} className="btn-back">
              &lt; Back
            </button>
          ) : (
            <div />
          )}

          <button
            onClick={step === 4 ? submitQuiz : nextStep}
            className="btn-next"
          >
            {step === 4 ? "Find My HDB Match" : "Next >"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default QuizPage;