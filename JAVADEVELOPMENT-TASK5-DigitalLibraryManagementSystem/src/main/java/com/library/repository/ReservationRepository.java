package com.library.repository;

import com.library.model.Book;
import com.library.model.Reservation;
import com.library.model.ReservationStatus;
import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUser(User user);
    List<Reservation> findByBookAndStatusOrderByReservedAtAsc(Book book, ReservationStatus status);
    Optional<Reservation> findByUserAndBookAndStatusIn(User user, Book book, List<ReservationStatus> statuses);
    long countByBookAndStatusIn(Book book, List<ReservationStatus> statuses);
}
