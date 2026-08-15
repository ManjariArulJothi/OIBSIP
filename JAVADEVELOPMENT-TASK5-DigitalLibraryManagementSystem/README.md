# 📚 Digital Library Management System

A full-stack web application for managing a library's catalogue, book issuing/returns, fines, and advance
reservations, with separate **Admin** and **User** roles.

Built with **Java 21 + Spring Boot 3.2**, **Spring Security**, **Spring Data JPA**, **Thymeleaf**, and **H2 / MySQL**.

---

## ✅ Feature Checklist Coverage

**Admin Module**
- [x] Admin login with access to all system features (role-based, Spring Security)
- [x] Add new book: title, author, ISBN, category, quantity
- [x] Edit or delete existing book records
- [x] View all issued books and their due dates
- [x] View and manage all registered member accounts (enable/disable/delete)
- [x] Fine management: mark fines as paid

**User Module**
- [x] User registration and login
- [x] Browse book catalogue by category
- [x] Search for a specific book by title or author
- [x] Issue a book (decrements available quantity; records due date)
- [x] Return a book (increments available quantity)
- [x] Fine generation: automatically calculated if return is overdue (₹5/day, configurable)
- [x] Advance booking: reserve a book that is currently issued to another user (waitlist + auto-hold when a copy frees up)
- [x] Contact/query form (stored in DB, visible in the admin panel)

---

## 🏗️ Project Structure

```
library-management/
├── pom.xml
└── src/main/
    ├── java/com/library/
    │   ├── LibraryManagementApplication.java   # Spring Boot entry point
    │   ├── config/
    │   │   ├── SecurityConfig.java              # Spring Security: roles, login/logout, BCrypt
    │   │   └── DataInitializer.java             # Seeds default admin + sample books on first run
    │   ├── model/                               # JPA entities
    │   │   ├── User.java
    │   │   ├── Book.java
    │   │   ├── IssueRecord.java                 # one row per issue/return + fine info
    │   │   ├── Reservation.java                 # advance booking / waitlist
    │   │   ├── ContactMessage.java
    │   │   ├── Role.java, IssueStatus.java, ReservationStatus.java (enums)
    │   ├── repository/                          # Spring Data JPA repositories
    │   │   ├── UserRepository.java
    │   │   ├── BookRepository.java
    │   │   ├── IssueRecordRepository.java
    │   │   ├── ReservationRepository.java
    │   │   └── ContactMessageRepository.java
    │   ├── service/                             # business logic
    │   │   ├── UserService.java                 # registration + Spring Security UserDetailsService
    │   │   ├── BookService.java                 # catalogue CRUD, search, category filter
    │   │   ├── IssueService.java                # issue/return + automatic fine calculation
    │   │   ├── ReservationService.java           # waitlist, hold-and-promote, scheduled hold expiry
    │   │   └── ContactService.java
    │   └── controller/
    │       ├── AuthController.java              # login, register, home, contact (public)
    │       ├── AdminController.java             # /admin/** — books, issued books, reservations, members, fines, messages
    │       └── UserController.java              # /user/** — catalogue, issue/return, reservations
    └── resources/
        ├── application.properties               # DB config + business rules (fine/day, loan period)
        ├── static/css/style.css                 # Modern CSS design system
        └── templates/                           # Thymeleaf views
            ├── login.html, register.html, contact.html, access-denied.html
            ├── fragments/header.html             # shared navbars with active states
            ├── admin/  (dashboard, books, book-form, issued-books, reservations, members, fines, messages)
            └── user/   (dashboard, catalogue, my-books, reservations)
```

---

## ⚙️ How the core logic works

- **Issuing**: `IssueService.issueBook()` only succeeds if `availableQuantity > 0`; it decrements the count and
  creates an `IssueRecord` with `dueDate = issueDate + 14 days` (configurable).
- **Returning & fines**: `IssueService.returnBook()` stamps the return date, and if it's after the due date,
  calculates `overdueDays × ₹5/day` and stores it on the record. The admin can mark it paid from `/admin/fines`.
- **Advance booking**: if a book has zero copies available, a user can reserve it (`ReservationService.reserveBook`)
  instead of issuing it. When any copy of that book is returned, the earliest pending reservation is automatically
  promoted to `READY` and the copy is held for **48 hours** (configurable). A scheduled job
  (`@Scheduled` in `ReservationService`) runs hourly to expire unclaimed holds and offer the copy to the next
  person in line.
- **Security**: passwords are BCrypt-hashed; `/admin/**` requires `ROLE_ADMIN`, `/user/**` requires `ROLE_USER`.

---

## 🚀 Running the project

### Requirements
- JDK 17 or newer
- Maven 3.6+ (or use an IDE like IntelliJ/Eclipse/VS Code with Maven support)
- Internet access on first build (Maven needs to download dependencies from Maven Central)

### 1. Default mode — zero setup (H2 file database)
The project ships pre-configured to use an embedded **H2** database stored in `./data/librarydb.mv.db`,
so it runs with no external database installation:

```bash
cd library-management
mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

- The app auto-creates the schema (`ddl-auto=update`) and seeds:
  - Admin login: **admin@library.com** / **admin123**
  - 8 sample books across several categories
- You can inspect the DB directly at **http://localhost:8080/h2-console**
  (JDBC URL: `jdbc:h2:file:./data/librarydb`, user `sa`, blank password).

### 2. Switching to MySQL
1. Create the database: `CREATE DATABASE library_db;`
2. In `src/main/resources/application.properties`, comment out the H2 block and uncomment the MySQL block,
   filling in your username/password.
3. Run `mvn spring-boot:run` again — Hibernate will create the tables automatically.

### 3. Building a runnable JAR
```bash
mvn clean package
java -jar target/library-management-1.0.0.jar
```

---

## 🔑 Default accounts

| Role  | Email               | Password  |
|-------|---------------------|-----------|
| Admin | admin@library.com   | admin123  |
| User  | *(register your own via `/register`)* | — |

---

## 🧩 Configurable business rules

In `application.properties`:

```properties
library.fine.per-day=5              # ₹ fine charged per overdue day
library.loan.period-days=14         # loan duration when a book is issued
library.reservation.hold-hours=48   # how long a reserved copy is held before it's released
```

---

## ⚠️ Note on this build

This project was written and reviewed file-by-file in a sandboxed environment without access to Maven Central,
so it could not be compiled end-to-end before delivery. Every Java file was manually checked for structural
correctness (imports, brace/paren balance, class names matching filenames) and every Thymeleaf template was
checked for balanced tags. It should build cleanly with `mvn spring-boot:run` on a machine with normal internet
access — if you do hit a small compile error, it's most likely a minor typo that a first `mvn compile` will point
straight to.
