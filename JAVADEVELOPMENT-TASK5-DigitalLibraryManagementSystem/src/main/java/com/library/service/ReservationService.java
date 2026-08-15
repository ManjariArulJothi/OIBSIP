package com.library.service;

import com.library.model.*;
import com.library.repository.BookRepository;
import com.library.repository.IssueRecordRepository;
import com.library.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final IssueRecordRepository issueRecordRepository;

    @Value("${library.reservation.hold-hours:48}")
    private int holdHours;

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            Arrays.asList(ReservationStatus.PENDING, ReservationStatus.READY);

    public ReservationService(ReservationRepository reservationRepository,
                              BookRepository bookRepository,
                              IssueRecordRepository issueRecordRepository) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.issueRecordRepository = issueRecordRepository;
    }

    /**
     * A user reserves a book that is currently fully issued out to other users.
     * (If copies are actually available, the user should just issue it directly.)
     */
    @Transactional
    public Reservation reserveBook(User user, Book book) {
        if (book.isAvailable()) {
            throw new IllegalStateException("This book currently has copies available — please issue it directly instead of reserving.");
        }
        if (issueRecordRepository.findByBookAndUserAndStatus(book, user, IssueStatus.ISSUED).isPresent()) {
            throw new IllegalStateException("You already have an active loan for this book.");
        }
        reservationRepository.findByUserAndBookAndStatusIn(user, book, ACTIVE_STATUSES)
                .ifPresent(r -> { throw new IllegalStateException("You already have an active reservation for this book."); });

        Reservation reservation = new Reservation(user, book);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId, User requester) {
        Reservation reservation = getById(reservationId);
        if (!reservation.getUser().getId().equals(requester.getId()) && requester.getRole() != Role.ADMIN) {
            throw new IllegalStateException("You may only cancel your own reservations.");
        }
        boolean wasReady = reservation.getStatus() == ReservationStatus.READY;
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        if (wasReady) {
            // release the held copy back to the pool and promote the next person in line
            Book book = reservation.getBook();
            book.setAvailableQuantity(book.getAvailableQuantity() + 1);
            bookRepository.save(book);
            promoteNextReservation(book);
        }
    }

    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
    }

    public List<Reservation> getReservationsForUser(User user) {
        return reservationRepository.findByUser(user);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Called after a book copy is returned. If someone is waiting, hold the copy for them.
     */
    @Transactional
    public void promoteNextReservation(Book book) {
        List<Reservation> queue = reservationRepository
                .findByBookAndStatusOrderByReservedAtAsc(book, ReservationStatus.PENDING);
        if (queue.isEmpty() || book.getAvailableQuantity() <= 0) {
            return;
        }
        Reservation next = queue.get(0);
        next.setStatus(ReservationStatus.READY);
        next.setHoldExpiresAt(LocalDateTime.now().plusHours(holdHours));
        reservationRepository.save(next);

        // hold the copy: remove it from the publicly available pool
        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        bookRepository.save(book);
    }

    /**
     * Runs hourly: expires holds nobody claimed in time, releases the copy,
     * and offers it to the next person in the reservation queue.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void expireStaleHolds() {
        List<Reservation> readyReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.READY)
                .filter(r -> r.getHoldExpiresAt() != null && r.getHoldExpiresAt().isBefore(LocalDateTime.now()))
                .toList();

        for (Reservation r : readyReservations) {
            r.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(r);

            Book book = r.getBook();
            book.setAvailableQuantity(book.getAvailableQuantity() + 1);
            bookRepository.save(book);

            promoteNextReservation(book);
        }
    }
}
