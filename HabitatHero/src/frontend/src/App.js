import './App.css';

import { Routes, Route } from "react-router-dom";
import NavigationBar from "./components/NavigationBar";
import HomePage from "./pages/HomePage";
import CreateAccountPage from "./pages/CreateAccountPage";
import LoginPage from './pages/LoginPage';
import Footer from "./components/Footer";
import QuizPage from "./pages/QuizPage";
import ResultsPage from './pages/ResultsPage';
import ProfilePage from './pages/ProfilePage';

function App() {
  return (
    <>
      <NavigationBar />

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/create-account" element={<CreateAccountPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/quiz" element={<QuizPage />} />
        <Route path="/results" element={<ResultsPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Routes>
      <Footer />
    </>
  );
}

export default App;