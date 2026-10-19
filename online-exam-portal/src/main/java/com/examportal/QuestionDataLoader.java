
package com.examportal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionDataLoader implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    public QuestionDataLoader(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {

        if (questionRepository.count() == 0) {

            List<Question> questions = List.of(
                    new Question(
                            "Which language runs on the Java Virtual Machine?",
                            "Java", "HTML", "CSS", "SQL", "A"
                    ),
                    new Question(
                            "Which keyword creates a class in Java?",
                            "function", "class", "define", "object", "B"
                    ),
                    new Question(
                            "Which one is a relational database?",
                            "HTML", "CSS", "MySQL", "JavaScript", "C"
                    ),
                    new Question(
                            "Which method is the usual Java program entry point?",
                            "start()", "run()", "main()", "init()", "C"
                    ),
                    new Question(
                            "What does SQL stand for?",
                            "Structured Query Language",
                            "Simple Question Language",
                            "Standard Quick Link",
                            "System Query List",
                            "A"
                    )
            );

            questionRepository.saveAll(questions);
            System.out.println("Sample exam questions inserted!");
        }
    }
}
