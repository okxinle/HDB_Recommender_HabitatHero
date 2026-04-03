import "../styles/ProfilePage.css";
import { User, Mail, Lock, Calendar } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem("token");
    const storedUser = localStorage.getItem("user");

    if (!token || !storedUser) {
      navigate("/login");
      return;
    }

    setUser(JSON.parse(storedUser));

    const fetchProfile = async () => {
      try {
        const response = await fetch("http://localhost:8080/api/profile", {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`
          }
        });

        const data = await response.json();

        if (data.status === "success") {
          setProfile(data.data);
        }
      } catch (error) {
        console.error("profile fetch failed");
      }
    };

    fetchProfile();
  }, [navigate]);

  return (
    <div className="profile-page">
      <div className="profile-header">
        <h1 className="profile-title">My Profile</h1>
        <p className="profile-subtitle">View your account details</p>
      </div>

      <div className="profile-card">
        <div className="profile-item">
          <div className="profile-icon">
            <User size={20} />
          </div>
          <div className="profile-info">
            <span className="profile-label">Name</span>
            <span className="profile-value">
              {profile?.name || "Not available"}
            </span>
          </div>
        </div>

        <div className="profile-item">
          <div className="profile-icon">
            <Mail size={20} />
          </div>
          <div className="profile-info">
            <span className="profile-label">Email Address</span>
            <span className="profile-value">
              {user?.email}
            </span>
          </div>
        </div>

        <div className="profile-item">
          <div className="profile-icon">
            <Lock size={20} />
          </div>
          <div className="profile-info">
            <span className="profile-label">Password</span>
            <div className="password-row">
              <span className="profile-value">********</span>
              <button className="change-password-btn">
                Change Password
              </button>
            </div>
          </div>
        </div>

        <div className="profile-item">
          <div className="profile-icon">
            <Calendar size={20} />
          </div>
          <div className="profile-info">
            <span className="profile-label">Account created on</span>
            <span className="profile-value">
              {user?.createdAt || "Not available"}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ProfilePage;