
package com.examportal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final QuestionRepository questionRepository;
    private final AppUserRepository appUserRepository;
    private final ExamResultRepository examResultRepository;

    public AdminController(
            QuestionRepository questionRepository,
            AppUserRepository appUserRepository,
            ExamResultRepository examResultRepository) {
        this.questionRepository = questionRepository;
        this.appUserRepository = appUserRepository;
        this.examResultRepository = examResultRepository;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("username", "Administrator");
        model.addAttribute("totalQuestions", questionRepository.count());
        model.addAttribute("totalUsers", appUserRepository.count());
        model.addAttribute("totalAttempts", examResultRepository.count());
        return "admin-dashboard";
    }

    @GetMapping("/admin/questions")
    public String manageQuestions(Model model) {
        var questions = questionRepository.findAll();

        System.out.println("QUESTIONS FOUND: " + questions.size());
        System.out.println("QUESTION DATA: " + questions);

        model.addAttribute("questions", questions);
        return "admin-questions";
    }
    @PostMapping("/admin/questions/add")
    public String addQuestion(
            @RequestParam String questionText,
            @RequestParam String optionA,
            @RequestParam String optionB,
            @RequestParam String optionC,
            @RequestParam String optionD,
            @RequestParam String correctAnswer,
            RedirectAttributes redirectAttributes) {

        if (questionText.isBlank()
                || optionA.isBlank()
                || optionB.isBlank()
                || optionC.isBlank()
                || optionD.isBlank()
                || correctAnswer.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Please fill in all question fields.");
            return "redirect:/admin/questions";
        }

        String answer = correctAnswer.trim().toUpperCase();

        if (!answer.matches("[ABCD]")) {
            redirectAttributes.addFlashAttribute(
                    "error", "Correct answer must be A, B, C, or D.");
            return "redirect:/admin/questions";
        }

        Question question = new Question();
        question.setQuestionText(questionText.trim());
        question.setOptionA(optionA.trim());
        question.setOptionB(optionB.trim());
        question.setOptionC(optionC.trim());
        question.setOptionD(optionD.trim());
        question.setCorrectAnswer(answer);

        questionRepository.save(question);

        redirectAttributes.addFlashAttribute(
                "message", "Question added successfully!");

        return "redirect:/admin/questions";
    }

    @PostMapping("/admin/questions/delete")
    public String deleteQuestion(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        if (questionRepository.existsById(id)) {
            questionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute(
                    "message", "Question deleted successfully!");
        }

        return "redirect:/admin/questions";
    }

    @GetMapping("/admin/results")
    public String viewAllResults(Model model) {
        model.addAttribute("results", examResultRepository.findAll());
        return "admin-results";
    }
}
