package habitathero.boundary;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.auth.JwtService;
import habitathero.control.UserProfileDbManager;
import habitathero.entity.HDBBlock;
import habitathero.entity.UserAccount;
import habitathero.entity.UserProfile;
import habitathero.repository.UserProfileRepository;
import habitathero.repository.UserRepository;


@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    // --- Dependencies ---
    // From HEAD
    private final UserProfileDbManager userProfileDbManager;

    // From prototype-v1.0
    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // Constructor from HEAD
    public ProfileController(UserProfileDbManager userProfileDbManager) {
        this.userProfileDbManager = userProfileDbManager;
    }

    // --- SECURITY HELPER (From prototype-v1.0) ---
    // Extracts the user securely from the JWT wristband
    private UserAccount getAuthenticatedUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        // Extract email/username from token
        String email = jwtService.extractEmail(token); 
        return userRepository.findByEmail(email).orElse(null);
    }

    // --- ENDPOINTS ---

    // 1. RESULTS ENDPOINTS (From HEAD)
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

    @PostMapping("/results")
    public ResponseEntity<?> importSavedResults(@RequestBody java.util.List<HDBBlock> results, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserAccount userAccount)) {
            return ResponseEntity.status(401).body(Map.of(
                "status", "unauthorized",
                "message", "Please log in to save results."
            ));
        }

        userProfileDbManager.saveLatestResults(userAccount.getUserId(), results == null ? java.util.List.of() : results);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // 2. PROFILE ENDPOINTS (From prototype-v1.0)
    @GetMapping
    public ResponseEntity<?> getSavedProfile(@RequestHeader("Authorization") String token) {
        UserAccount user = getAuthenticatedUser(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        // Fetch the profile using the user's ID
        UserProfile profile = profileRepository.findById(user.getUserId()).orElse(null);

        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);
        data.put("name", user.getName());
        data.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    @PostMapping
    public ResponseEntity<?> saveProfile(@RequestHeader("Authorization") String token, @RequestBody UserProfile payload) {
        UserAccount user = getAuthenticatedUser(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        // SECURITY OVERRIDE: Ignore any userId sent by the frontend.
        // Force the profile ID to match the authenticated token's owner.
        payload.setUserId(user.getUserId());
        
        // Save to PostgreSQL. If the ID exists, it runs an UPDATE. If new, it runs an INSERT.
        profileRepository.save(payload);
        
        return ResponseEntity.ok(Map.of("status", "success", "message", "Profile saved successfully"));
    }
}