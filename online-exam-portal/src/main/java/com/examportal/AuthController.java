
package com.examportal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String createAccount(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        username = username.trim();

        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("error",
                    "Username and password are required.");
            return "register";
        }

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error",
                    "That username already exists.");
            return "register";
        }

        AppUser appUser = new AppUser();
        appUser.setUsername(username);
        appUser.setPassword(passwordEncoder.encode(password));
        appUser.setRole("STUDENT");

        userRepository.save(appUser);

        return "redirect:/login?registered";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }


}
