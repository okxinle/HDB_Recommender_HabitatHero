package habitathero.entity;

import java.time.LocalDateTime;

public class UserAccount {

    private int userID;
    private String email;
    private String passwordHash;
    private LocalDateTime createdAt;

    public UserAccount(int userID, String email, String passwordHash, LocalDateTime createdAt) {
        this.userID = userID;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public int getUserID() {
        return this.userID;
    }

    public String getEmail() {
        return this.email;
    }

}