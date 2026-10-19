
package com.examportal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class DashboardController {

    private final ExamResultRepository examResultRepository;

    public DashboardController(ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        String username = authentication.getName();

        // Preserve the existing admin redirection.
        if (authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        // Check whether this student has already submitted an exam.
        boolean examTaken = !examResultRepository
                .findByUsernameOrderBySubmittedAtDesc(username)
                .isEmpty();

        model.addAttribute("username", username);
        model.addAttribute("examTaken", examTaken);

        return "dashboard";
    }
}
