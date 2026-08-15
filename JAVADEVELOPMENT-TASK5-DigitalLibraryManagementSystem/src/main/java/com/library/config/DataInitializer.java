package com.library.config;

import com.library.model.Book;
import com.library.model.Role;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, BookRepository bookRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@library.com").isEmpty()) {
            User admin = new User("Library Admin", "admin@library.com",
                    passwordEncoder.encode("admin123"), Role.ADMIN);
            userRepository.save(admin);
            System.out.println("==> Default admin created: admin@library.com / admin123");
        }

        if (bookRepository.count() == 0) {
            bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884", "Programming", 3));
            bookRepository.save(new Book("The Pragmatic Programmer", "David Thomas", "9780135957059", "Programming", 2));
            bookRepository.save(new Book("Introduction to Algorithms", "Thomas H. Cormen", "9780262046305", "Computer Science", 2));
            bookRepository.save(new Book("Sapiens", "Yuval Noah Harari", "9780062316097", "History", 4));
            bookRepository.save(new Book("Atomic Habits", "James Clear", "9780735211292", "Self Help", 5));
            bookRepository.save(new Book("The Pragmatic Thinker", "Andy Hunt", "9780974514055", "Programming", 1));
            bookRepository.save(new Book("A Brief History of Time", "Stephen Hawking", "9780553380163", "Science", 3));
            bookRepository.save(new Book("Wuthering Heights", "Emily Bronte", "9780141439556", "Fiction", 2));
            System.out.println("==> Sample book catalogue seeded (" + bookRepository.count() + " titles)");
        }
    }
}
