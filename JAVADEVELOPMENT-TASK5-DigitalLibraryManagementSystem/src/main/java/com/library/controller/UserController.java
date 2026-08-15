package com.library.controller;

import com.library.model.*;
import com.library.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    private final BookService bookService;
    private final IssueService issueService;
    private final UserService userService;
    private final ReservationService reservationService;

    public UserController(BookService bookService, IssueService issueService,
                           UserService userService, ReservationService reservationService) {
        this.bookService = bookService;
        this.issueService = issueService;
        this.userService = userService;
        this.reservationService = reservationService;
    }

    private User currentUser(Authentication authentication) {
        return userService.getByEmail(authentication.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User user = currentUser(authentication);
        List<IssueRecord> active = issueService.getActiveRecordsForUser(user);
        model.addAttribute("activeBooks", active);
        model.addAttribute("totalIssued", active.size());
        model.addAttribute("outstandingFine", active.stream()
                .mapToDouble(r -> r.isOverdue()
                        ? java.time.temporal.ChronoUnit.DAYS.between(r.getDueDate(), java.time.LocalDate.now()) * issueService.getFinePerDay()
                        : 0.0).sum());
        model.addAttribute("activeReservations", reservationService.getReservationsForUser(user).stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.READY)
                .count());
        return "user/dashboard";
    }

    // ---------- Catalogue browsing & search ----------

    @GetMapping("/catalogue")
    public String catalogue(@RequestParam(required = false) String category,
                             @RequestParam(required = false) String q,
                             Model model,
                             Authentication authentication) {
        User user = currentUser(authentication);
        List<Book> books;
        if (q != null && !q.isBlank()) {
            books = bookService.search(q);
        } else if (category != null && !category.isBlank()) {
            books = bookService.getByCategory(category);
        } else {
            books = bookService.getAllBooks();
        }

        List<Long> issuedBookIds = issueService.getActiveRecordsForUser(user).stream()
                .map(r -> r.getBook().getId())
                .toList();

        List<Long> reservedBookIds = reservationService.getReservationsForUser(user).stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.READY)
                .map(r -> r.getBook().getId())
                .toList();

        model.addAttribute("books", books);
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("query", q);
        model.addAttribute("issuedBookIds", issuedBookIds);
        model.addAttribute("reservedBookIds", reservedBookIds);
        return "user/catalogue";
    }

    // ---------- Issue / Return ----------

    @PostMapping("/books/issue/{bookId}")
    public String issueBook(@PathVariable Long bookId, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Book book = bookService.getById(bookId);
        try {
            issueService.issueBook(user, book);
            redirectAttributes.addFlashAttribute("successMessage", "\"" + book.getTitle() + "\" has been issued to you. Enjoy the read!");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/catalogue";
    }

    @PostMapping("/books/return/{recordId}")
    public String returnBook(@PathVariable Long recordId, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        IssueRecord record = issueService.getById(recordId);
        if (!record.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "That record does not belong to you.");
            return "redirect:/user/my-books";
        }
        IssueRecord returned = issueService.returnBook(record);
        if (returned.getFineAmount() > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Book returned. A fine of \u20B9" + returned.getFineAmount() + " has been recorded for the overdue return.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Book returned on time. Thanks!");
        }
        return "redirect:/user/my-books";
    }

    @GetMapping("/my-books")
    public String myBooks(Model model, Authentication authentication) {
        User user = currentUser(authentication);
        model.addAttribute("records", issueService.getRecordsForUser(user));
        model.addAttribute("finePerDay", issueService.getFinePerDay());
        return "user/my-books";
    }

    // ---------- Advance booking / reservations ----------

    @PostMapping("/books/reserve/{bookId}")
    public String reserveBook(@PathVariable Long bookId, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        Book book = bookService.getById(bookId);
        try {
            reservationService.reserveBook(user, book);
            redirectAttributes.addFlashAttribute("successMessage",
                    "You're on the waitlist for \"" + book.getTitle() + "\". We'll hold a copy for you when it's your turn.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/catalogue";
    }

    @GetMapping("/reservations")
    public String reservations(Model model, Authentication authentication) {
        User user = currentUser(authentication);
        model.addAttribute("reservations", reservationService.getReservationsForUser(user));
        return "user/reservations";
    }

    @PostMapping("/reservations/cancel/{id}")
    public String cancelReservation(@PathVariable Long id, Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        try {
            reservationService.cancelReservation(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/reservations";
    }

    @PostMapping("/reservations/claim/{id}")
    public String claimReservation(@PathVariable Long id, Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        try {
            Reservation reservation = reservationService.getById(id);
            issueService.issueReservedBook(reservation, user);
            redirectAttributes.addFlashAttribute("successMessage", "Reserved book issued to you. Enjoy!");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/reservations";
    }
}
