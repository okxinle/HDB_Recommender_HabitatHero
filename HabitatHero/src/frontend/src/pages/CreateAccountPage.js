import { useState } from "react"; // Added for state management
import { Link, useNavigate } from "react-router-dom"; // Added for redirection
import "../styles/CreateAccountPage.css";
import HDBAccount from "../assets/hdb_account.png";
import InputField from "../components/InputField";

function CreateAccountPage() {
  const navigate = useNavigate();

  // 1. State for registration form data
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // 2. Handle Registration (UC-01)
  const handleSubmit = async (e) => {
    e.preventDefault();

    // --- TESTING LOGIC START ---
    console.log("Mocking Registration for UC-01:", formData);
    
    // In a real scenario, you'd await fetch("/api/auth/register", ...) here.
    // For testing, we simulate a successful 200 OK response.
    
    alert("Account created successfully! Please log in.");
    
    // Redirect to the Login Page as requested
    navigate("/login"); 
    // --- TESTING LOGIC END ---
  };

  return (
    <main className="create-page">
      <div className="create-container">
        <section className="create-left">
          <h1 className="create-title">Create New Account</h1>
          <p className="create-subtitle">
            No account with us? Create an account to save your results!
          </p>
          <p className="auth-switch-text auth-switch-top">
            Already have an account?{" "}
            <Link to="/login" className="auth-switch-link">Log in</Link>
          </p>

          <form className="create-form" onSubmit={handleSubmit}>
            <InputField
              label="Name"
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange} // Capturing input
            />

            <InputField
              label="Email Address"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleInputChange} // Capturing input
            />

            <InputField
              label="Password"
              type="password"
              name="password"
              value={formData.password}
              onChange={handleInputChange} // Capturing input
            />

            <button type="submit" className="register-btn">
              REGISTER
            </button>
          </form>
        </section>

        <section className="create-right">
          <img
            src={HDBAccount}
            alt="HDB Background"
            className="create-image"
          />
        </section>
      </div>
    </main>
  );
}

export default CreateAccountPage;