package com.library.service;

import com.library.model.*;
import com.library.repository.BookRepository;
import com.library.repository.IssueRecordRepository;
import com.library.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class IssueService {

    private final IssueRecordRepository issueRecordRepository;
    private final BookRepository bookRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Value("${library.loan.period-days:14}")
    private int loanPeriodDays;

    @Value("${library.fine.per-day:5}")
    private double finePerDay;

    public IssueService(IssueRecordRepository issueRecordRepository,
                         BookRepository bookRepository,
                         ReservationRepository reservationRepository,
                         ReservationService reservationService) {
        this.issueRecordRepository = issueRecordRepository;
        this.bookRepository = bookRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
    }

    /** Issue a book directly from the open catalogue (copies available). */
    @Transactional
    public IssueRecord issueBook(User user, Book book) {
        if (issueRecordRepository.findByBookAndUserAndStatus(book, user, IssueStatus.ISSUED).isPresent()) {
            throw new IllegalStateException("You already have an active loan for \"" + book.getTitle() + "\". Please return it before issuing another copy.");
        }
        if (book.getAvailableQuantity() <= 0) {
            throw new IllegalStateException("No copies of this book are currently available. You can place a reservation instead.");
        }
        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        bookRepository.save(book);

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(loanPeriodDays);
        IssueRecord record = new IssueRecord(user, book, issueDate, dueDate);
        return issueRecordRepository.save(record);
    }

    /** Claim a book that became available through a fulfilled reservation (READY status). */
    @Transactional
    public IssueRecord issueReservedBook(Reservation reservation, User user) {
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("This reservation does not belong to you.");
        }
        if (reservation.getStatus() != ReservationStatus.READY) {
            throw new IllegalStateException("This reservation is not ready for pickup.");
        }
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(loanPeriodDays);
        // Note: availableQuantity was already decremented when the hold was created
        IssueRecord record = new IssueRecord(user, reservation.getBook(), issueDate, dueDate);
        record = issueRecordRepository.save(record);

        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);
        return record;
    }

    /** Return a book, calculating any overdue fine automatically. */
    @Transactional
    public IssueRecord returnBook(IssueRecord record) {
        if (record.getStatus() == IssueStatus.RETURNED) {
            throw new IllegalStateException("This book has already been returned.");
        }
        LocalDate returnDate = LocalDate.now();
        record.setReturnDate(returnDate);
        record.setStatus(IssueStatus.RETURNED);

        if (returnDate.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
            record.setFineAmount(overdueDays * finePerDay);
        }
        issueRecordRepository.save(record);

        Book book = record.getBook();
        book.setAvailableQuantity(book.getAvailableQuantity() + 1);
        bookRepository.save(book);

        // offer the copy to the next person in the reservation queue, if any
        reservationService.promoteNextReservation(book);

        return record;
    }

    public List<IssueRecord> getAllIssuedBooks() {
        return issueRecordRepository.findByStatus(IssueStatus.ISSUED);
    }

    public List<IssueRecord> getRecordsForUser(User user) {
        return issueRecordRepository.findByUser(user);
    }

    public List<IssueRecord> getActiveRecordsForUser(User user) {
        return issueRecordRepository.findByUserAndStatus(user, IssueStatus.ISSUED);
    }

    public IssueRecord getById(Long id) {
        return issueRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue record not found: " + id));
    }

    public List<IssueRecord> getUnpaidFines() {
        return issueRecordRepository.findByFineAmountGreaterThanAndFinePaidFalse(0.0);
    }

    @Transactional
    public void markFinePaid(Long recordId) {
        IssueRecord record = getById(recordId);
        record.setFinePaid(true);
        issueRecordRepository.save(record);
    }

    public double getFinePerDay() {
        return finePerDay;
    }
}
