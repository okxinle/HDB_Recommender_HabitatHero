import { useState } from "react";
import "../styles/QuizPage.css";
// You will create these component files next
import StructuralConstraints from "../components/StructuralConstraints";
import LivabilityFactors from "../components/LivabilityFactors";
import CommuterAnalysis from "../components/CommuterAnalysis";
import QuizSummary from "../components/QuizSummary";

function QuizPage() {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    budget: [400000, 450000],
    towns: [],
    flatTypes: [],
    minLease: 70,
    factors: {}, // For REQ-1.3
    commuters: { destA: "", destB: "" } // For REQ-1.5
  });

  const nextStep = () => setStep((prev) => prev + 1);
  const prevStep = () => setStep((prev) => prev - 1);

  const renderStep = () => {
    switch (step) {
      case 1: return <StructuralConstraints data={formData} update={setFormData} />;
      case 2: return <LivabilityFactors data={formData} update={setFormData} />;
      case 3: return <CommuterAnalysis data={formData} update={setFormData} />;
      case 4: return <QuizSummary data={formData} />;
      default: return <StructuralConstraints />;
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