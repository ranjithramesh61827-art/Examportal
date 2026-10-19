
package com.examportal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class ExamController {

    private final QuestionRepository questionRepository;
    private final ExamResultRepository examResultRepository;

    public ExamController(
            QuestionRepository questionRepository,
            ExamResultRepository examResultRepository) {
        this.questionRepository = questionRepository;
        this.examResultRepository = examResultRepository;
    }

    @GetMapping("/exam")
    public String showExam(
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();

        // Prevent students who already submitted from taking the exam again.
        if (!examResultRepository
                .findByUsernameOrderBySubmittedAtDesc(username).isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message",
                    "You have already taken the exam. Retakes are not allowed."
            );
            return "redirect:/results";
        }

        List<Question> questions = questionRepository.findAll();

        if (questions.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message",
                    "No exam questions are available yet."
            );
            return "redirect:/dashboard";
        }

        model.addAttribute("questions", questions);
        model.addAttribute("submitted", false);

        return "exam";
    }

    @PostMapping("/submit")
    public String submitExam(
            @RequestParam Map<String, String> answers,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();

        // Check again during submission to prevent a second attempt.
        if (!examResultRepository
                .findByUsernameOrderBySubmittedAtDesc(username).isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message",
                    "You have already taken the exam. Retakes are not allowed."
            );
            return "redirect:/results";
        }

        List<Question> questions = questionRepository.findAll();
        int score = 0;

        for (Question question : questions) {
            String selectedAnswer = answers.get("question_" + question.getId());

            if (selectedAnswer != null
                    && selectedAnswer.equalsIgnoreCase(
                    question.getCorrectAnswer())) {
                score++;
            }
        }

        ExamResult result = new ExamResult();
        result.setUsername(username);
        result.setScore(score);
        result.setTotalQuestions(questions.size());
        result.setSubmittedAt(LocalDateTime.now());

        examResultRepository.save(result);

        model.addAttribute("questions", questions);
        model.addAttribute("submitted", true);
        model.addAttribute("score", score);
        model.addAttribute("total", questions.size());

        return "exam";
    }
}
