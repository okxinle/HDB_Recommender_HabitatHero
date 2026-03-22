package habitathero.boundary;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.auth.JwtService;
import habitathero.entity.UserAccount;
import habitathero.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AccountController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtService jwtService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 1. The Registration Endpoint
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Registration Invalid. Account already exists"));
        }

        UserAccount newUser = new UserAccount();
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password)); 
        
        userRepository.save(newUser);
        
        // Automatically generate token upon registration
        String token = jwtService.generateToken(newUser);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "token", token,
            "user", Map.of("email", newUser.getEmail(), "isActive", newUser.isActive())
        ));
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

        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", token,
                "user", Map.of("email", user.getEmail(), "isActive", user.isActive())
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password"));
        }
    }
}