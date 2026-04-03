import "../styles/ProfilePage.css";
import { User, Mail, Lock, Calendar } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [user, setUser] = useState(null);
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [passwordForm, setPasswordForm] = useState({ oldPassword: "", newPassword: "" });
  const [passwordError, setPasswordError] = useState("");
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

  // Format date if available
  const formatDate = (dateString) => {
    if (!dateString) return "Not available";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("en-US", {
        year: "numeric",
        month: "long",
        day: "numeric"
      });
    } catch {
      return dateString;
    }
  };

  const handlePasswordInputChange = (e) => {
    const { name, value } = e.target;
    setPasswordForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleChangePassword = async () => {
    setPasswordError("");
    const token = localStorage.getItem("token");

    if (!token) {
      navigate("/login");
      return;
    }

    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      setPasswordError("Please fill in both fields.");
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/api/auth/change-password", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
      });

      const data = await response.json();

      if (response.ok && data.status === "success") {
        alert("Password updated successfully!");
        setPasswordForm({ oldPassword: "", newPassword: "" });
        setShowPasswordForm(false);
      } else {
        setPasswordError(data.message || "Failed to update password.");
      }
    } catch (error) {
      setPasswordError("System error. Please try again later.");
    }
  };

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
              <button
                className="change-password-btn"
                onClick={() => setShowPasswordForm((prev) => !prev)}
              >
                Change Password
              </button>
            </div>
            {showPasswordForm && (
              <div style={{ marginTop: "12px", display: "grid", gap: "8px", maxWidth: "320px" }}>
                <input
                  type="password"
                  name="oldPassword"
                  placeholder="Old password"
                  value={passwordForm.oldPassword}
                  onChange={handlePasswordInputChange}
                />
                <input
                  type="password"
                  name="newPassword"
                  placeholder="New password"
                  value={passwordForm.newPassword}
                  onChange={handlePasswordInputChange}
                />
                <button className="change-password-btn" onClick={handleChangePassword}>
                  Update Password
                </button>
                {passwordError && <span style={{ color: "#b91c1c" }}>{passwordError}</span>}
              </div>
            )}
          </div>
        </div>

        <div className="profile-item">
          <div className="profile-icon">
            <Calendar size={20} />
          </div>
          <div className="profile-info">
            <span className="profile-label">Account created on</span>
            <span className="profile-value">
              {formatDate(profile?.createdAt || user?.createdAt)}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ProfilePage;