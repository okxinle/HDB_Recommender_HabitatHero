import "../styles/ProfilePage.css";
import { User, Mail, Lock, Calendar } from "lucide-react";

function QuizPage() {
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
                    {/* <span className="profile-value">{user.name}</span> */}
                </div>
                </div>

                <div className="profile-item">
                <div className="profile-icon">
                    <Mail size={20} />
                </div>
                <div className="profile-info">
                    <span className="profile-label">Email Address</span>
                    {/* <span className="profile-value">{user.email}</span> */}
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
                    {/* <span className="profile-value">{user.createdAt}</span> */}
                </div>
                </div>
            </div>
            </div>
  );
}

export default QuizPage;