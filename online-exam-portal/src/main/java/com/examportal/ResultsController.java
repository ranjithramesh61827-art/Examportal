
package com.examportal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ResultsController {

    private final ExamResultRepository examResultRepository;

    public ResultsController(
            ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }

    @GetMapping("/results")
    public String viewResults(
            Authentication authentication,
            Model model) {

        List<ExamResult> results =
                examResultRepository
                        .findByUsernameOrderBySubmittedAtDesc(
                                authentication.getName());

        model.addAttribute("results", results);

        return "results";
    }
}
