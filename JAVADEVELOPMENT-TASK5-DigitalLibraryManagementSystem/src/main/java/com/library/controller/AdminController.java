package com.library.controller;

import com.library.model.Book;
import com.library.model.User;
import com.library.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookService bookService;
    private final IssueService issueService;
    private final UserService userService;
    private final ContactService contactService;
    private final ReservationService reservationService;

    public AdminController(BookService bookService, IssueService issueService, UserService userService,
                            ContactService contactService, ReservationService reservationService) {
        this.bookService = bookService;
        this.issueService = issueService;
        this.userService = userService;
        this.contactService = contactService;
        this.reservationService = reservationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalBooks", bookService.getAllBooks().size());
        model.addAttribute("totalIssued", issueService.getAllIssuedBooks().size());
        model.addAttribute("totalMembers", userService.getAllMembers().size());
        model.addAttribute("unpaidFines", issueService.getUnpaidFines().size());
        model.addAttribute("pendingReservations", reservationService.getAllReservations().size());
        model.addAttribute("newMessages", contactService.getAll().stream().filter(m -> !m.isResolved()).count());
        return "admin/dashboard";
    }

    // ---------- Book CRUD ----------

    @GetMapping("/books")
    public String listBooks(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String category,
                            Model model) {
        List<Book> books;
        if (q != null && !q.isBlank()) {
            books = bookService.search(q);
        } else if (category != null && !category.isBlank()) {
            books = bookService.getByCategory(category);
        } else {
            books = bookService.getAllBooks();
        }
        model.addAttribute("books", books);
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("query", q);
        return "admin/books";
    }

    @GetMapping("/books/new")
    public String newBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", bookService.getAllCategories());
        return "admin/book-form";
    }

    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getById(id));
        model.addAttribute("categories", bookService.getAllCategories());
        return "admin/book-form";
    }

    @PostMapping("/books/save")
    public String saveBook(@Valid @ModelAttribute("book") Book book,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", bookService.getAllCategories());
            return "admin/book-form";
        }
        try {
            if (book.getId() == null) {
                bookService.addBook(book);
                redirectAttributes.addFlashAttribute("successMessage", "Book \"" + book.getTitle() + "\" added successfully.");
            } else {
                bookService.updateBook(book.getId(), book);
                redirectAttributes.addFlashAttribute("successMessage", "Book \"" + book.getTitle() + "\" updated successfully.");
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", bookService.getAllCategories());
            return "admin/book-form";
        }
        return "redirect:/admin/books";
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Book book = bookService.getById(id);
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("successMessage", "Book \"" + book.getTitle() + "\" has been deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete book: " + ex.getMessage());
        }
        return "redirect:/admin/books";
    }

    // ---------- Issued books ----------

    @GetMapping("/issued-books")
    public String issuedBooks(Model model) {
        model.addAttribute("records", issueService.getAllIssuedBooks());
        return "admin/issued-books";
    }

    // ---------- Reservations ----------

    @GetMapping("/reservations")
    public String reservations(Model model) {
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "admin/reservations";
    }

    @PostMapping("/reservations/cancel/{id}")
    public String cancelReservation(@PathVariable Long id, Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User admin = userService.getByEmail(authentication.getName());
        try {
            reservationService.cancelReservation(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/reservations";
    }

    // ---------- Members ----------

    @GetMapping("/members")
    public String members(Model model) {
        model.addAttribute("members", userService.getAllMembers());
        return "admin/members";
    }

    @PostMapping("/members/toggle/{id}")
    public String toggleMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        var user = userService.getById(id);
        boolean newStatus = !user.isEnabled();
        userService.setEnabled(id, newStatus);
        redirectAttributes.addFlashAttribute("successMessage",
                "Account for " + user.getFullName() + (newStatus ? " enabled." : " disabled."));
        return "redirect:/admin/members";
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            var user = userService.getById(id);
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Member account for " + user.getFullName() + " deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete member: " + ex.getMessage());
        }
        return "redirect:/admin/members";
    }

    // ---------- Fines ----------

    @GetMapping("/fines")
    public String fines(Model model) {
        model.addAttribute("fines", issueService.getUnpaidFines());
        return "admin/fines";
    }

    @PostMapping("/fines/mark-paid/{recordId}")
    public String markFinePaid(@PathVariable Long recordId, RedirectAttributes redirectAttributes) {
        issueService.markFinePaid(recordId);
        redirectAttributes.addFlashAttribute("successMessage", "Fine marked as paid.");
        return "redirect:/admin/fines";
    }

    // ---------- Contact messages ----------

    @GetMapping("/messages")
    public String messages(Model model) {
        model.addAttribute("messages", contactService.getAll());
        return "admin/messages";
    }

    @PostMapping("/messages/resolve/{id}")
    public String resolveMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        contactService.markResolved(id);
        redirectAttributes.addFlashAttribute("successMessage", "Message marked as resolved.");
        return "redirect:/admin/messages";
    }
}

