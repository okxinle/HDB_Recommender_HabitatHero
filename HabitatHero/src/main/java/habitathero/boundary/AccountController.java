package habitathero.boundary;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.auth.JwtService;
import habitathero.control.AuthService;
import habitathero.entity.UserAccount;
import habitathero.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AccountController {

    // --- Dependencies ---
    private final AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    // 1. The Registration Endpoint
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        try {
            UserAccount newUser = authService.registerUser(email, password, name);
            String token = authService.generateToken(newUser);

            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("userId", newUser.getUserId());
            userPayload.put("email", newUser.getEmail());
            userPayload.put("name", newUser.getName());
            userPayload.put("createdAt", newUser.getCreatedAt());
            userPayload.put("isActive", newUser.isActive());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", token,
                "user", userPayload
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. The Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        UserAccount user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }

        // --- LOCKOUT CHECK START ---
        // Check if the user is currently locked out
        if (user.getLockTime() != null) {
            // Define the lockout duration (e.g., 15 minutes)
            LocalDateTime lockExpiration = user.getLockTime().plusMinutes(15);
            
            if (LocalDateTime.now().isBefore(lockExpiration)) {
                return ResponseEntity.status(403).body(Map.of(
                    "message", "Account locked due to too many failed attempts. Please try again in 15 minutes."
                ));
            } else {
                // The lock duration has passed, unlock the account
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            }
        }
        // --- LOCKOUT CHECK END ---

        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            // SUCCESSFUL LOGIN: Reset attempts and clear any locks
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);

            String token = jwtService.generateToken(user);

            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("userId", user.getUserId());
            userPayload.put("email", user.getEmail());
            userPayload.put("name", user.getName());
            userPayload.put("createdAt", user.getCreatedAt());
            userPayload.put("isActive", user.isActive());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", token,
                "user", userPayload
            ));
        } else {
            // FAILED LOGIN: Increment attempts
            int newAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(newAttempts);

            // Lock the account if attempts reach 5
            if (newAttempts >= 5) {
                user.setLockTime(LocalDateTime.now());
                userRepository.save(user);
                return ResponseEntity.status(403).body(Map.of(
                    "message", "Account locked due to 5 failed attempts. Please try again in 15 minutes."
                ));
            }

            userRepository.save(user);
            return ResponseEntity.status(401).body(Map.of(
                "message", "Invalid email or password. Attempts remaining: " + (5 - newAttempts)
            ));
        }
    }
}