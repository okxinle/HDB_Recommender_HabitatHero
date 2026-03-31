package habitathero.boundary;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.control.AuthService;
import habitathero.entity.UserAccount;

@RestController
@RequestMapping("/api/auth")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    // 1. The Registration Endpoint
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        try {
            UserAccount newUser = authService.registerUser(email, password);
            String token = authService.generateToken(newUser);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", token,
                "user", Map.of(
                    "userId", newUser.getUserId(),
                    "email", newUser.getEmail(),
                    "isActive", newUser.isActive()
                )
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

        try {
            UserAccount user = authService.authenticateUser(email, password);
            String token = authService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", token,
                "user", Map.of(
                    "userId", user.getUserId(),
                    "email", user.getEmail(),
                    "isActive", user.isActive()
                )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }
    }
}