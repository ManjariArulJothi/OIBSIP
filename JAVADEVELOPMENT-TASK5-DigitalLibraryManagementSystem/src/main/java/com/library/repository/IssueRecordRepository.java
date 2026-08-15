package com.library.repository;

import com.library.model.Book;
import com.library.model.IssueRecord;
import com.library.model.IssueStatus;
import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {
    List<IssueRecord> findByStatus(IssueStatus status);
    List<IssueRecord> findByUser(User user);
    List<IssueRecord> findByUserAndStatus(User user, IssueStatus status);
    Optional<IssueRecord> findByBookAndUserAndStatus(Book book, User user, IssueStatus status);
    long countByBookAndStatus(Book book, IssueStatus status);
    List<IssueRecord> findByFineAmountGreaterThanAndFinePaidFalse(double amount);
}
