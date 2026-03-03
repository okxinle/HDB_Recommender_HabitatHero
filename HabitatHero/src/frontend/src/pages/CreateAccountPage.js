import "../styles/CreateAccountPage.css";
import HDBAccount from "../assets/hdb_account.png";
import InputField from "../components/InputField";

function CreateAccountPage() {
  return (
     <main className="create-page">
      <div className="create-container">
    
        <section className="create-left">
          <h1 className="create-title">Create New Account</h1>
          <p className="create-subtitle">
            No account with us? Create an account to save your results!
          </p>

          <form className="create-form">
            <InputField
              label="Name"
              type="text"
              name="name"
            />

            <InputField
              label="Email Address"
              type="email"
              name="email"
            />

            <InputField
              label="Password"
              type="password"
              name="password"
            />

            <button type="submit" className="register-btn">
              REGISTER
            </button>
          </form>
        </section>

        <section className="create-right">
          <img src={HDBAccount}
            alt="HDB Background"
            className="create-image"
          />
        </section>

      </div>
    </main>
  );
}

export default CreateAccountPage;