import { useState } from "react";
import "../styles/QuizPage.css";
import StructuralConstraints from "../components/StructuralConstraints";
import LivabilityFactors from "../components/LivabilityFactors";
import CommuterAnalysis from "../components/CommuterAnalysis";
import QuizSummary from "../components/QuizSummary";

function QuizPage() {
  const [step, setStep] = useState(1);
  const [attemptedNext, setAttemptedNext] = useState(false);

  const [formData, setFormData] = useState({
    budget: [400000, 450000],
    towns: [],
    flatTypes: [],
    minLease: 50,
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

  const isStep1Valid =
    formData.budget[0] < formData.budget[1] &&
    formData.towns.length > 0 &&
    formData.flatTypes.length > 0 &&
    formData.minLease >= 50;

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
    setStep((prev) => prev + 1);
  };

  const prevStep = () => {
    setAttemptedNext(false);
    setStep((prev) => prev - 1);
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
          "Summary of Preferences",
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

          <button onClick={nextStep} className="btn-next">
            {step === 4 ? "Submit" : "Next >"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default QuizPage;