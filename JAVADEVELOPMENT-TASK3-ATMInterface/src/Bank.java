import java.util.*;

/**
 * Represents the central banking system.
 * Manages accounts, authenticates users and administrators, coordinates
 * inter-account transactions, controls the ATM Cash Vault, and persists state.
 */
public class Bank {

    public static final String ADMIN_ID = "admin";
    public static final String ADMIN_PIN = "9999";

    private final Map<String, Account> accounts;
    private final CashVault cashVault;
    private final StorageManager storageManager;

    public Bank() {
        this(new StorageManager(), new CashVault());
    }

    public Bank(StorageManager storageManager, CashVault cashVault) {
        this.storageManager = storageManager;
        this.cashVault = cashVault;
        this.accounts = new HashMap<>();

        loadOrSeedData();
    }

    private void loadOrSeedData() {
        Map<String, Account> loaded = storageManager.load(cashVault);
        if (loaded != null && !loaded.isEmpty()) {
            accounts.putAll(loaded);
        } else {
            seedSampleAccounts();
            save();
        }
    }

    /**
     * Pre-seeds initial demo accounts when first run.
     */
    private void seedSampleAccounts() {
        Account acc1 = new Account("1001", "Manjari PA", "1234", 5000.00, Account.AccountType.SAVINGS, Account.Status.ACTIVE, 20000.0, 0.0, null);
        Account acc2 = new Account("1002", "Arjun Kumar", "4321", 12500.50, Account.AccountType.SAVINGS, Account.Status.ACTIVE, 25000.0, 0.0, null);
        Account acc3 = new Account("1003", "Divya Sree", "1111", 750.00, Account.AccountType.SAVINGS, Account.Status.ACTIVE, 15000.0, 0.0, null);
        Account acc4 = new Account("1004", "TechCorp Enterprise", "5555", 85000.00, Account.AccountType.CURRENT, Account.Status.ACTIVE, 100000.0, 0.0, null);

        accounts.put(acc1.getAccountId(), acc1);
        accounts.put(acc2.getAccountId(), acc2);
        accounts.put(acc3.getAccountId(), acc3);
        accounts.put(acc4.getAccountId(), acc4);
    }

    public synchronized void save() {
        storageManager.save(accounts, cashVault);
    }

    public CashVault getCashVault() {
        return cashVault;
    }

    public synchronized Account findAccount(String accountId) {
        return accounts.get(accountId);
    }

    public synchronized boolean accountExists(String accountId) {
        return accounts.containsKey(accountId);
    }

    public synchronized Collection<Account> getAllAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    /**
     * Authenticates an administrator.
     */
    public boolean authenticateAdmin(String adminId, String pin) {
        return ADMIN_ID.equalsIgnoreCase(adminId) && ADMIN_PIN.equals(pin);
    }

    /**
     * Result of an authentication attempt.
     */
    public static class AuthResult {
        private final boolean success;
        private final boolean accountLocked;
        private final int remainingAttempts;
        private final Account account;
        private final String message;

        public AuthResult(boolean success, boolean accountLocked, int remainingAttempts, Account account, String message) {
            this.success = success;
            this.accountLocked = accountLocked;
            this.remainingAttempts = remainingAttempts;
            this.account = account;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isAccountLocked() {
            return accountLocked;
        }

        public int getRemainingAttempts() {
            return remainingAttempts;
        }

        public Account getAccount() {
            return account;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Verifies User ID & PIN with lockout security.
     */
    public synchronized AuthResult authenticate(String accountId, String pin) {
        Account account = accounts.get(accountId);
        if (account == null) {
            return new AuthResult(false, false, 0, null, "Account ID does not exist.");
        }

        if (account.isLocked()) {
            return new AuthResult(false, true, 0, account, "Account is LOCKED due to excessive failed attempts. Contact administrator.");
        }

        if (account.checkPin(pin)) {
            account.resetFailedLogins();
            save();
            return new AuthResult(true, false, 3, account, "Login successful.");
        } else {
            account.recordFailedLogin();
            save();
            if (account.isLocked()) {
                return new AuthResult(false, true, 0, account, "Account has been LOCKED after 3 consecutive failed login attempts.");
            } else {
                int remaining = 3 - account.getFailedLoginAttempts();
                return new AuthResult(false, false, remaining, null, "Incorrect PIN. Attempts remaining: " + remaining);
            }
        }
    }

    /**
     * Registers a new account.
     */
    public synchronized boolean createAccount(String accountId, String ownerName, String pin, double openingBalance, Account.AccountType type) {
        if (accountId == null || accountId.trim().isEmpty() || accounts.containsKey(accountId)) {
            return false;
        }
        if (pin == null || !pin.matches("\\d{4,6}")) {
            return false;
        }
        if (openingBalance < 0) {
            return false;
        }

        double dailyLimit = (type == Account.AccountType.CURRENT) ? 50000.0 : 20000.0;
        Account newAcc = new Account(accountId.trim(), ownerName.trim(), pin.trim(), 0.0, type, Account.Status.ACTIVE, dailyLimit, 0.0, null);
        if (openingBalance > 0) {
            newAcc.deposit(openingBalance, "Initial opening deposit");
        }
        accounts.put(accountId.trim(), newAcc);
        save();
        return true;
    }

    /**
     * Unlocks an account (admin action).
     */
    public synchronized boolean unlockAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account != null) {
            account.unlock();
            save();
            return true;
        }
        return false;
    }

    /**
     * Transfers funds atomically between two accounts.
     */
    public synchronized boolean transfer(Account from, String toAccountId, double amount) {
        if (from == null || toAccountId == null || amount <= 0) {
            return false;
        }
        if (from.getAccountId().equalsIgnoreCase(toAccountId)) {
            return false; // Self transfer not allowed
        }
        Account to = accounts.get(toAccountId);
        if (to == null) {
            return false; // Recipient not found
        }
        if (!from.hasSufficientFunds(amount)) {
            return false; // Insufficient balance
        }

        from.recordTransferOut(amount, to.getAccountId(), to.getOwnerName());
        to.recordTransferIn(amount, from.getAccountId(), from.getOwnerName());
        save();
        return true;
    }

    /**
     * Aggregates total deposits across all active customer accounts.
     */
    public synchronized double getTotalBankDeposits() {
        return accounts.values().stream().mapToDouble(Account::getBalance).sum();
    }

    /**
     * Returns total transactions executed across all accounts.
     */
    public synchronized int getTotalTransactionsCount() {
        return accounts.values().stream().mapToInt(a -> a.getHistory().size()).sum();
    }
}
