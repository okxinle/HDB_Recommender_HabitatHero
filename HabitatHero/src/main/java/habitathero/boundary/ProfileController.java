package habitathero.boundary;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.auth.JwtService;
import habitathero.entity.UserAccount;
import habitathero.entity.UserProfile;
import habitathero.repository.UserProfileRepository;
import habitathero.repository.UserRepository;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    // --- SECURITY HELPER ---
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

    @GetMapping
    public ResponseEntity<?> getSavedProfile(@RequestHeader("Authorization") String token) {
        UserAccount user = getAuthenticatedUser(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        // Fetch the profile using the user's ID
        UserProfile profile = profileRepository.findById(user.getUserId()).orElse(null);

        if (profile == null) {
            return ResponseEntity.ok(Map.of("status", "success", "data", null));
        }

        return ResponseEntity.ok(Map.of("status", "success", "data", profile));
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