package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
    }

    public Book addBook(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("A book with this ISBN already exists.");
        }
        book.setAvailableQuantity(book.getTotalQuantity());
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updated) {
        Book existing = getById(id);

        if (bookRepository.existsByIsbnAndIdNot(updated.getIsbn(), id)) {
            throw new IllegalArgumentException("Another book already has this ISBN.");
        }

        // if total quantity changes, adjust available quantity by the same delta
        int delta = updated.getTotalQuantity() - existing.getTotalQuantity();
        int newAvailable = existing.getAvailableQuantity() + delta;

        if (newAvailable < 0) {
            throw new IllegalArgumentException("Cannot reduce total copies below the number of currently issued copies.");
        }

        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setIsbn(updated.getIsbn());
        existing.setCategory(updated.getCategory());
        existing.setTotalQuantity(updated.getTotalQuantity());
        existing.setAvailableQuantity(newAvailable);

        return bookRepository.save(existing);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    public List<Book> getByCategory(String category) {
        return bookRepository.findByCategoryIgnoreCase(category);
    }

    public List<Book> search(String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query);
    }

    public List<String> getAllCategories() {
        return bookRepository.findAllCategories();
    }
}
