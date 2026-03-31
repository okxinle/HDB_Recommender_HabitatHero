package habitathero.boundary;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.control.UserProfileDbManager;
import habitathero.entity.HDBBlock;
import habitathero.entity.UserAccount;
import habitathero.entity.UserProfile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileDbManager userProfileDbManager;

    public ProfileController(UserProfileDbManager userProfileDbManager) {
        this.userProfileDbManager = userProfileDbManager;
    }

    @GetMapping("/results")
    public ResponseEntity<?> getSavedResults(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserAccount userAccount)) {
            return ResponseEntity.status(401).body(Map.of(
                "status", "unauthorized",
                "message", "Please log in to view saved results."
            ));
        }

        java.util.List<HDBBlock> results = userProfileDbManager.getLatestResults(userAccount.getUserId());
        return ResponseEntity.ok(Map.of("status", "success", "results", results));
    }

    @PostMapping
    public ResponseEntity<?> saveProfile(@RequestBody UserProfile payload) {
        // Because of your entity structure, Spring Boot automatically converts 
        // the incoming JSON into your UserProfile object here!
        
        // TODO: Save 'payload' to the database linked to the authenticated user
        // profileService.saveProfileData(payload);
        
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}