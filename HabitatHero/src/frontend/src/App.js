import './App.css';

import { useEffect } from "react";
import { Routes, Route } from "react-router-dom";
import NavigationBar from "./components/NavigationBar";
import HomePage from "./pages/HomePage";
import CreateAccountPage from "./pages/CreateAccountPage";
import LoginPage from './pages/LoginPage';
import Footer from "./components/Footer";
import QuizPage from "./pages/QuizPage";
import HDBResultDashBoardPage from './pages/HDBResultDashBoardPage';
import ProfilePage from './pages/ProfilePage';
import ResourcesPage from './pages/ResourcesPage';
import SpatialAnalysisResultDashBoardPage from './pages/SpatialAnalysisResultDashBoardPage';

function App() {
  useEffect(() => {
    const navEntry = performance.getEntriesByType("navigation")[0];
    if (navEntry && navEntry.type === "reload") {
      sessionStorage.removeItem("temporaryGuestResults");
    }
  }, []);

  return (
    <div className="app-wrapper">
      <NavigationBar />

      <main className="main-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/create-account" element={<CreateAccountPage />} />
          <Route path="/signup" element={<CreateAccountPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/quiz" element={<QuizPage />} />
          <Route path="/results" element={<HDBResultDashBoardPage />} />
          <Route path="/resources" element={<ResourcesPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/result-detail/:blockId" element={<SpatialAnalysisResultDashBoardPage />} />
        </Routes>
      </main>

      <Footer />
    </div>
  );
}

export default App;