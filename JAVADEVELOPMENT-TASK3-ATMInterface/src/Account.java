import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a customer's bank account.
 * Encapsulates financial state, security status (lock/unlock, failed logins),
 * daily transaction limits, and a comprehensive transaction audit trail.
 */
public class Account {

    public enum AccountType {
        SAVINGS,
        CURRENT
    }

    public enum Status {
        ACTIVE,
        LOCKED,
        SUSPENDED
    }

    private final String accountId;
    private final String ownerName;
    private String pin;
    private double balance;
    private AccountType accountType;
    private Status status;
    private int failedLoginAttempts;
    private double dailyWithdrawalLimit;
    private double dailyWithdrawnToday;
    private LocalDate lastWithdrawalDate;
    private final List<Transaction> history;

    public Account(String accountId, String ownerName, String pin, double openingBalance) {
        this(accountId, ownerName, pin, openingBalance, AccountType.SAVINGS, Status.ACTIVE, 20000.0, 0.0, LocalDate.now());
    }

    public Account(String accountId, String ownerName, String pin, double openingBalance,
                   AccountType accountType, Status status, double dailyLimit,
                   double dailyWithdrawnToday, LocalDate lastWithdrawalDate) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.pin = pin;
        this.balance = openingBalance;
        this.accountType = accountType != null ? accountType : AccountType.SAVINGS;
        this.status = status != null ? status : Status.ACTIVE;
        this.failedLoginAttempts = 0;
        this.dailyWithdrawalLimit = dailyLimit > 0 ? dailyLimit : (this.accountType == AccountType.CURRENT ? 50000.0 : 20000.0);
        this.dailyWithdrawnToday = dailyWithdrawnToday;
        this.lastWithdrawalDate = lastWithdrawalDate != null ? lastWithdrawalDate : LocalDate.now();
        this.history = new ArrayList<>();
    }

    // ---------- Getters & Setters ----------

    public String getAccountId() {
        return accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getPin() {
        return pin;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public double getDailyWithdrawalLimit() {
        return dailyWithdrawalLimit;
    }

    public void setDailyWithdrawalLimit(double dailyWithdrawalLimit) {
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
    }

    public double getDailyWithdrawnToday() {
        refreshDailyLimitIfNeeded();
        return dailyWithdrawnToday;
    }

    public LocalDate getLastWithdrawalDate() {
        return lastWithdrawalDate;
    }

    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void addHistoricalTransaction(Transaction t) {
        history.add(t);
    }

    // ---------- Security & Authentication ----------

    public boolean isLocked() {
        return status == Status.LOCKED;
    }

    public void lock() {
        this.status = Status.LOCKED;
    }

    public void unlock() {
        this.status = Status.ACTIVE;
        this.failedLoginAttempts = 0;
    }

    public boolean checkPin(String enteredPin) {
        return this.pin != null && this.pin.equals(enteredPin);
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 3) {
            this.status = Status.LOCKED;
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
    }

    /**
     * Changes account PIN after validating format and ensuring it differs from old PIN.
     */
    public boolean changePin(String oldPin, String newPin) {
        if (!checkPin(oldPin)) {
            return false;
        }
        if (newPin == null || !newPin.matches("\\d{4,6}")) {
            return false;
        }
        if (oldPin.equals(newPin)) {
            return false;
        }
        this.pin = newPin;
        history.add(new Transaction(Transaction.Type.PIN_CHANGE, 0.0, balance, "Security PIN updated successfully"));
        return true;
    }

    // ---------- Daily Limit Handling ----------

    private synchronized void refreshDailyLimitIfNeeded() {
        LocalDate today = LocalDate.now();
        if (lastWithdrawalDate == null || !lastWithdrawalDate.equals(today)) {
            dailyWithdrawnToday = 0.0;
            lastWithdrawalDate = today;
        }
    }

    public synchronized double getRemainingDailyLimit() {
        refreshDailyLimitIfNeeded();
        return Math.max(0.0, dailyWithdrawalLimit - dailyWithdrawnToday);
    }

    public synchronized boolean hasSufficientFunds(double amount) {
        return amount > 0 && amount <= balance;
    }

    public synchronized boolean isWithinDailyLimit(double amount) {
        refreshDailyLimitIfNeeded();
        return (dailyWithdrawnToday + amount) <= dailyWithdrawalLimit;
    }

    // ---------- Core Banking Operations ----------

    public synchronized Transaction deposit(double amount, String description) {
        return deposit(amount, description, null);
    }

    public synchronized Transaction deposit(double amount, String description, String noteBreakdown) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
        Transaction t = new Transaction(Transaction.Type.DEPOSIT, amount, balance, description, noteBreakdown);
        history.add(t);
        return t;
    }

    public synchronized Transaction withdraw(double amount, String description) {
        return withdraw(amount, description, null, Transaction.Type.WITHDRAWAL);
    }

    public synchronized Transaction withdraw(double amount, String description, String noteBreakdown) {
        return withdraw(amount, description, noteBreakdown, Transaction.Type.WITHDRAWAL);
    }

    public synchronized Transaction withdraw(double amount, String description, String noteBreakdown, Transaction.Type txnType) {
        if (amount <= 0) {
            return null;
        }
        refreshDailyLimitIfNeeded();
        if (!hasSufficientFunds(amount) || !isWithinDailyLimit(amount)) {
            return null;
        }

        balance -= amount;
        dailyWithdrawnToday += amount;
        Transaction t = new Transaction(txnType != null ? txnType : Transaction.Type.WITHDRAWAL, amount, balance, description, noteBreakdown);
        history.add(t);
        return t;
    }

    public synchronized Transaction recordTransferOut(double amount, String toAccountId, String toOwnerName) {
        if (amount <= 0 || !hasSufficientFunds(amount)) {
            return null;
        }
        balance -= amount;
        String desc = "Transfer to " + toAccountId + " (" + toOwnerName + ")";
        Transaction t = new Transaction(Transaction.Type.TRANSFER_OUT, amount, balance, desc);
        history.add(t);
        return t;
    }

    public synchronized Transaction recordTransferIn(double amount, String fromAccountId, String fromOwnerName) {
        if (amount <= 0) {
            return null;
        }
        balance += amount;
        String desc = "Transfer from " + fromAccountId + " (" + fromOwnerName + ")";
        Transaction t = new Transaction(Transaction.Type.TRANSFER_IN, amount, balance, desc);
        history.add(t);
        return t;
    }

    public List<Transaction> getRecentTransactions(int count) {
        if (history.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        int start = Math.max(0, history.size() - count);
        return Collections.unmodifiableList(new ArrayList<>(history.subList(start, history.size())));
    }
}
