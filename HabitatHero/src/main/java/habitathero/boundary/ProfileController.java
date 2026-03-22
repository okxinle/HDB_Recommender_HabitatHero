package habitathero.boundary;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.entity.UserProfile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    // You will eventually inject your database service here:
    // @Autowired
    // private ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getSavedProfile(@RequestHeader("Authorization") String token) {
        // TODO: Extract user ID/email from the token, then query your database
        UserProfile profile = null; // Placeholder for your database call

        // If the user is new and hasn't saved a profile yet, return null data
        if (profile == null) {
            return ResponseEntity.ok(Map.of("status", "success", "data", null));
        }

        // Otherwise, return the populated UserProfile
        return ResponseEntity.ok(Map.of("status", "success", "data", profile));
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