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
      {/* Step Indicator (Progress Bar) */}
      <div className="quiz-stepper">
        {[1, 2, 3, 4].map((num) => (
          <div key={num} className={`step-item ${step === num ? "active" : ""} ${step > num ? "completed" : ""}`}>
            <span className="step-number">{num}</span>
            <span className="step-label">
              {num === 1 && "Structural Constraints"}
              {num === 2 && "Livability Factors"}
              {num === 3 && "Multi-Commuter Analysis"}
              {num === 4 && "Summary of Preferences"}
            </span>
          </div>
        ))}
      </div>

      <div className="quiz-card-content">
        {renderStep()}
        
        <div className="quiz-actions">
          {step > 1 && <button onClick={prevStep} className="btn-back">Back</button>}
          <button onClick={nextStep} className="btn-next">
            {step === 4 ? "Submit" : "Next >"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default QuizPage;