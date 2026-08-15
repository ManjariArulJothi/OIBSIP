import java.util.*;

/**
 * Interactive Console Interface for the ATM System.
 * Coordinates multi-session navigation, customer banking operations,
 * receipt generation, and administrator management tools.
 */
public class ATM {

    private final Bank bank;
    private final Scanner scanner;

    public ATM(Bank bank) {
        this.bank = bank;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Top-level entry loop for the ATM kiosk.
     */
    public void start() {
        boolean powerOn = true;
        while (powerOn) {
            ConsoleUI.printBanner();
            System.out.println("\n" + ConsoleUI.CYAN + "Please select a service option:" + ConsoleUI.RESET);
            System.out.println("  [1] Insert Card & Customer Login");
            System.out.println("  [2] Quick Fast Cash");
            System.out.println("  [3] Open / Register New Account");
            System.out.println("  [4] Bank Administrator Portal");
            System.out.println("  [5] Power Down / Exit");
            System.out.print(ConsoleUI.YELLOW + "\nSelect Option (1-5): " + ConsoleUI.RESET);

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleCustomerLoginFlow(false);
                    break;
                case "2":
                    handleCustomerLoginFlow(true);
                    break;
                case "3":
                    handleAccountRegistration();
                    break;
                case "4":
                    handleAdminPortalFlow();
                    break;
                case "5":
                    powerOn = false;
                    ConsoleUI.printInfo("Shutting down ATM Terminal. Goodbye!");
                    break;
                default:
                    ConsoleUI.printError("Invalid option. Please choose a valid number from 1 to 5.");
            }
        }
    }

    // ==========================================
    // CUSTOMER FLOW
    // ==========================================

    private void handleCustomerLoginFlow(boolean directToFastCash) {
        ConsoleUI.printHeader("Customer Authentication");
        System.out.print("Enter 4-Digit Account Number / User ID: ");
        String accountId = scanner.nextLine().trim();

        if (accountId.isEmpty()) {
            ConsoleUI.printError("Account ID cannot be blank.");
            return;
        }

        Account account = bank.findAccount(accountId);
        if (account == null) {
            ConsoleUI.printError("Account not found. Please verify your Account ID.");
            return;
        }

        if (account.isLocked()) {
            ConsoleUI.printError("This account is LOCKED due to previous failed attempts. Please contact bank staff.");
            return;
        }

        String pin = ConsoleUI.promptPin(scanner, "Enter Security PIN: ");
        Bank.AuthResult auth = bank.authenticate(accountId, pin);

        if (!auth.isSuccess()) {
            ConsoleUI.printError(auth.getMessage());
            return;
        }

        ConsoleUI.printSuccess("Welcome back, " + account.getOwnerName() + "!");

        if (directToFastCash) {
            handleFastCash(account);
        }

        runCustomerMenu(account);
    }

    private void runCustomerMenu(Account account) {
        boolean inSession = true;
        while (inSession) {
            System.out.println("\n" + ConsoleUI.CYAN + "╔══════════════════════════════════════════════════════════════════╗");
            System.out.printf("║  ACCOUNT: %-14s HOLDER: %-20s TYPE: %-8s║%n",
                    account.getAccountId(),
                    truncate(account.getOwnerName(), 20),
                    account.getAccountType());
            System.out.printf("║  AVAILABLE BALANCE: ₹%-43.2f║%n", account.getBalance());
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] Balance Inquiry            [5] Fund Transfer                ║");
            System.out.println("║  [2] Fast Cash                  [6] Mini-Statement & History     ║");
            System.out.println("║  [3] Cash Withdrawal            [7] Change Security PIN          ║");
            System.out.println("║  [4] Cash Deposit               [8] Account Profile & Limits     ║");
            System.out.println("║  [9] Logout & Return Card                                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝" + ConsoleUI.RESET);
            System.out.print(ConsoleUI.YELLOW + "Enter option (1-9): " + ConsoleUI.RESET);

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleBalanceInquiry(account);
                    break;
                case "2":
                    handleFastCash(account);
                    break;
                case "3":
                    handleWithdrawal(account);
                    break;
                case "4":
                    handleDeposit(account);
                    break;
                case "5":
                    handleTransfer(account);
                    break;
                case "6":
                    handleTransactionHistory(account);
                    break;
                case "7":
                    handleChangePin(account);
                    break;
                case "8":
                    handleAccountProfile(account);
                    break;
                case "9":
                    inSession = false;
                    ConsoleUI.printSuccess("Card ejected. Thank you for banking with us, " + account.getOwnerName() + "!");
                    break;
                default:
                    ConsoleUI.printError("Invalid option. Please enter a choice between 1 and 9.");
            }
        }
    }

    // ---------- Customer Operations ----------

    private void handleBalanceInquiry(Account account) {
        ConsoleUI.printHeader("Balance Inquiry Summary");
        System.out.println("  Account Number:           " + account.getAccountId());
        System.out.println("  Account Holder:           " + account.getOwnerName());
        System.out.println("  Account Type:             " + account.getAccountType());
        System.out.println("  Account Status:           " + ConsoleUI.GREEN + account.getStatus() + ConsoleUI.RESET);
        System.out.printf("  Current Available Balance: %s₹%.2f%s%n", ConsoleUI.GREEN + ConsoleUI.BOLD, account.getBalance(), ConsoleUI.RESET);
        System.out.printf("  Daily Withdrawal Limit:   ₹%.2f%n", account.getDailyWithdrawalLimit());
        System.out.printf("  Remaining Limit for Today: ₹%.2f%n", account.getRemainingDailyLimit());
        ConsoleUI.printDivider();
    }

    private void handleFastCash(Account account) {
        ConsoleUI.printHeader("Fast Cash Quick Withdrawal");
        System.out.println("  [1] ₹100       [2] ₹500");
        System.out.println("  [3] ₹1,000     [4] ₹2,000");
        System.out.println("  [5] ₹5,000     [6] Cancel");
        System.out.print(ConsoleUI.YELLOW + "Select Fast Cash amount (1-6): " + ConsoleUI.RESET);

        String option = scanner.nextLine().trim();
        double amount = switch (option) {
            case "1" -> 100.0;
            case "2" -> 500.0;
            case "3" -> 1000.0;
            case "4" -> 2000.0;
            case "5" -> 5000.0;
            default -> 0.0;
        };

        if (amount == 0.0) {
            ConsoleUI.printInfo("Fast Cash cancelled.");
            return;
        }

        executeWithdrawal(account, amount, Transaction.Type.FAST_CASH);
    }

    private void handleWithdrawal(Account account) {
        ConsoleUI.printHeader("Custom Cash Withdrawal");
        System.out.println(ConsoleUI.CYAN + "Note: Available ATM note denominations: ₹500, ₹200, ₹100" + ConsoleUI.RESET);
        System.out.printf("Remaining Daily Limit: ₹%.2f | Balance: ₹%.2f%n",
                account.getRemainingDailyLimit(), account.getBalance());

        Double amount = promptAmount("Enter amount to withdraw (multiples of ₹100): ");
        if (amount == null) return;

        if (amount % 100 != 0) {
            ConsoleUI.printError("Amount must be a multiple of ₹100.");
            return;
        }

        executeWithdrawal(account, amount, Transaction.Type.WITHDRAWAL);
    }

    private void executeWithdrawal(Account account, double amount, Transaction.Type type) {
        if (!account.hasSufficientFunds(amount)) {
            ConsoleUI.printError(String.format("Insufficient account balance. Available: ₹%.2f", account.getBalance()));
            return;
        }

        if (!account.isWithinDailyLimit(amount)) {
            ConsoleUI.printError(String.format("Withdrawal exceeds daily limit. Remaining limit today: ₹%.2f", account.getRemainingDailyLimit()));
            return;
        }

        CashVault vault = bank.getCashVault();
        if (!vault.canDispense(amount)) {
            ConsoleUI.printError("ATM vault cannot dispense this exact amount due to current note availability. Please try a different amount.");
            return;
        }

        // Dispense cash
        Map<Integer, Integer> dispensed = vault.dispenseNotes(amount);
        String notesDesc = CashVault.formatNoteBreakdown(dispensed);

        Transaction txn = account.withdraw(amount, (type == Transaction.Type.FAST_CASH ? "Fast Cash Withdrawal" : "ATM Cash Withdrawal"), notesDesc, type);
        bank.save();

        ConsoleUI.printSuccess(String.format("Please collect your cash: ₹%.2f", amount));
        System.out.println(ConsoleUI.GREEN + "  Dispensed Notes: " + notesDesc + ConsoleUI.RESET);
        System.out.printf("  Updated Balance: ₹%.2f%n", account.getBalance());

        promptReceipt(account, txn);
    }

    private void handleDeposit(Account account) {
        ConsoleUI.printHeader("Cash Deposit");
        System.out.println("  [1] Direct Amount Entry");
        System.out.println("  [2] Count by Currency Notes (₹500 / ₹200 / ₹100)");
        System.out.println("  [3] Cancel");
        System.out.print(ConsoleUI.YELLOW + "Choose deposit mode (1-3): " + ConsoleUI.RESET);

        String mode = scanner.nextLine().trim();
        double amount = 0.0;
        String noteDesc = null;

        if ("1".equals(mode)) {
            Double entered = promptAmount("Enter total deposit amount: ₹");
            if (entered == null) return;
            amount = entered;
        } else if ("2".equals(mode)) {
            System.out.print("Enter count of ₹500 notes: ");
            int n500 = promptInt();
            System.out.print("Enter count of ₹200 notes: ");
            int n200 = promptInt();
            System.out.print("Enter count of ₹100 notes: ");
            int n100 = promptInt();

            amount = (n500 * 500.0) + (n200 * 200.0) + (n100 * 100.0);
            if (amount <= 0) {
                ConsoleUI.printError("Total deposited amount must be greater than zero.");
                return;
            }
            bank.getCashVault().depositNotes(n500, n200, n100);
            Map<Integer, Integer> map = new LinkedHashMap<>();
            if (n500 > 0) map.put(500, n500);
            if (n200 > 0) map.put(200, n200);
            if (n100 > 0) map.put(100, n100);
            noteDesc = CashVault.formatNoteBreakdown(map);
        } else {
            ConsoleUI.printInfo("Deposit cancelled.");
            return;
        }

        Transaction txn = account.deposit(amount, "ATM Cash Deposit", noteDesc);
        bank.save();

        ConsoleUI.printSuccess(String.format("Deposit of ₹%.2f successful!", amount));
        if (noteDesc != null) {
            System.out.println("  Deposited Notes: " + noteDesc);
        }
        System.out.printf("  Updated Balance: ₹%.2f%n", account.getBalance());

        promptReceipt(account, txn);
    }

    private void handleTransfer(Account account) {
        ConsoleUI.printHeader("Inter-Account Funds Transfer");
        System.out.print("Enter Recipient Account ID: ");
        String toAccountId = scanner.nextLine().trim();

        if (toAccountId.equalsIgnoreCase(account.getAccountId())) {
            ConsoleUI.printError("You cannot transfer money to your own account.");
            return;
        }

        Account recipient = bank.findAccount(toAccountId);
        if (recipient == null) {
            ConsoleUI.printError("Recipient account ID does not exist.");
            return;
        }

        System.out.println(ConsoleUI.CYAN + "▶ Recipient Verification:" + ConsoleUI.RESET);
        System.out.println("  Name:           " + recipient.getOwnerName());
        System.out.println("  Account Number: " + recipient.getAccountId());
        System.out.println("  Account Type:   " + recipient.getAccountType());

        Double amount = promptAmount("Enter amount to transfer: ₹");
        if (amount == null) return;

        if (!account.hasSufficientFunds(amount)) {
            ConsoleUI.printError(String.format("Insufficient funds. Available: ₹%.2f", account.getBalance()));
            return;
        }

        System.out.print(ConsoleUI.YELLOW + String.format("Confirm transfer of ₹%.2f to %s (A/C %s)? (Y/N): ",
                amount, recipient.getOwnerName(), recipient.getAccountId()) + ConsoleUI.RESET);
        String confirm = scanner.nextLine().trim();
        if (!"Y".equalsIgnoreCase(confirm) && !"YES".equalsIgnoreCase(confirm)) {
            ConsoleUI.printInfo("Transfer aborted by user.");
            return;
        }

        boolean success = bank.transfer(account, toAccountId, amount);
        if (success) {
            ConsoleUI.printSuccess(String.format("Transferred ₹%.2f to %s successfully!", amount, recipient.getOwnerName()));
            System.out.printf("  New Available Balance: ₹%.2f%n", account.getBalance());

            List<Transaction> hist = account.getHistory();
            Transaction lastTxn = hist.get(hist.size() - 1);
            promptReceipt(account, lastTxn);
        } else {
            ConsoleUI.printError("Transfer failed. Please check balance and try again.");
        }
    }

    private void handleTransactionHistory(Account account) {
        ConsoleUI.printHeader("Transaction History & Mini-Statement");
        System.out.println("  [1] View Last 5 Transactions (Mini-Statement)");
        System.out.println("  [2] View Full Transaction History");
        System.out.println("  [3] Filter by Deposits Only");
        System.out.println("  [4] Filter by Withdrawals & Fast Cash Only");
        System.out.println("  [5] Filter by Transfers Only");
        System.out.print(ConsoleUI.YELLOW + "Select option (1-5): " + ConsoleUI.RESET);

        String option = scanner.nextLine().trim();
        List<Transaction> list = switch (option) {
            case "1" -> account.getRecentTransactions(5);
            case "3" -> account.getHistory().stream().filter(t -> t.getType() == Transaction.Type.DEPOSIT || t.getType() == Transaction.Type.TRANSFER_IN).toList();
            case "4" -> account.getHistory().stream().filter(t -> t.getType() == Transaction.Type.WITHDRAWAL || t.getType() == Transaction.Type.FAST_CASH).toList();
            case "5" -> account.getHistory().stream().filter(t -> t.getType() == Transaction.Type.TRANSFER_IN || t.getType() == Transaction.Type.TRANSFER_OUT).toList();
            default -> account.getHistory();
        };

        System.out.println();
        ConsoleUI.printTransactionTable(list);
    }

    private void handleChangePin(Account account) {
        ConsoleUI.printHeader("Change Security PIN");
        String oldPin = ConsoleUI.promptPin(scanner, "Enter Current PIN: ");
        if (!account.checkPin(oldPin)) {
            ConsoleUI.printError("Current PIN is incorrect.");
            return;
        }

        String newPin = ConsoleUI.promptPin(scanner, "Enter New PIN (4-6 digits): ");
        if (newPin == null || !newPin.matches("\\d{4,6}")) {
            ConsoleUI.printError("Invalid PIN format. PIN must be 4 to 6 numeric digits.");
            return;
        }
        if (newPin.equals(oldPin)) {
            ConsoleUI.printError("New PIN cannot be the same as your current PIN.");
            return;
        }

        String confirmPin = ConsoleUI.promptPin(scanner, "Confirm New PIN: ");
        if (!newPin.equals(confirmPin)) {
            ConsoleUI.printError("PINs do not match. Change PIN aborted.");
            return;
        }

        boolean ok = account.changePin(oldPin, newPin);
        if (ok) {
            bank.save();
            ConsoleUI.printSuccess("Your ATM Security PIN has been updated successfully!");
        } else {
            ConsoleUI.printError("Failed to update PIN. Please verify requirements and try again.");
        }
    }

    private void handleAccountProfile(Account account) {
        ConsoleUI.printHeader("Account Profile Details");
        System.out.println("  Account Number:           " + account.getAccountId());
        System.out.println("  Account Holder Name:      " + account.getOwnerName());
        System.out.println("  Account Scheme:           " + account.getAccountType());
        System.out.println("  Account Status:           " + account.getStatus());
        System.out.printf("  Current Available Funds:  ₹%.2f%n", account.getBalance());
        System.out.printf("  Daily Withdrawal Cap:     ₹%.2f%n", account.getDailyWithdrawalLimit());
        System.out.printf("  Withdrawn Today:          ₹%.2f%n", account.getDailyWithdrawnToday());
        System.out.printf("  Remaining Limit Today:    ₹%.2f%n", account.getRemainingDailyLimit());
        System.out.println("  Total Lifetime Txns:      " + account.getHistory().size());
        ConsoleUI.printDivider();
    }

    private void promptReceipt(Account account, Transaction txn) {
        System.out.print(ConsoleUI.YELLOW + "\nWould you like a transaction receipt slip? (Y/N): " + ConsoleUI.RESET);
        String ans = scanner.nextLine().trim();
        if ("Y".equalsIgnoreCase(ans) || "YES".equalsIgnoreCase(ans)) {
            ConsoleUI.printAndSaveReceipt(account, txn);
        }
    }

    // ==========================================
    // REGISTRATION & ADMIN PORTAL
    // ==========================================

    private void handleAccountRegistration() {
        ConsoleUI.printHeader("Open New Bank Account");
        System.out.print("Enter Desired Account ID (e.g. 1005): ");
        String accId = scanner.nextLine().trim();

        if (accId.isEmpty() || bank.accountExists(accId)) {
            ConsoleUI.printError("Account ID is already taken or invalid. Please choose another.");
            return;
        }

        System.out.print("Enter Full Customer Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            ConsoleUI.printError("Customer name cannot be empty.");
            return;
        }

        String pin = ConsoleUI.promptPin(scanner, "Set 4-Digit Security PIN: ");
        if (!pin.matches("\\d{4,6}")) {
            ConsoleUI.printError("PIN must be 4 to 6 numeric digits.");
            return;
        }

        System.out.println("Select Account Type: [1] SAVINGS (Default)  [2] CURRENT");
        System.out.print("Choice (1/2): ");
        String typeChoice = scanner.nextLine().trim();
        Account.AccountType type = "2".equals(typeChoice) ? Account.AccountType.CURRENT : Account.AccountType.SAVINGS;

        Double openingBalance = promptAmount("Enter Initial Opening Deposit Amount (Min ₹500): ₹");
        if (openingBalance == null || openingBalance < 500) {
            ConsoleUI.printError("Minimum opening deposit is ₹500.00.");
            return;
        }

        boolean created = bank.createAccount(accId, name, pin, openingBalance, type);
        if (created) {
            ConsoleUI.printSuccess(String.format("Account %s created successfully for %s!", accId, name));
            ConsoleUI.printInfo("You can now login immediately using Account ID: " + accId);
        } else {
            ConsoleUI.printError("Could not create account. Please check inputs.");
        }
    }

    private void handleAdminPortalFlow() {
        ConsoleUI.printHeader("Bank Administrator Login");
        System.out.print("Enter Admin ID: ");
        String adminId = scanner.nextLine().trim();
        String pin = ConsoleUI.promptPin(scanner, "Enter Admin Passcode: ");

        if (!bank.authenticateAdmin(adminId, pin)) {
            ConsoleUI.printError("Unauthorized access. Invalid Admin credentials.");
            return;
        }

        ConsoleUI.printSuccess("Admin authentication granted.");
        boolean adminActive = true;

        while (adminActive) {
            System.out.println("\n" + ConsoleUI.MAGENTA + "╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    BANK MANAGER ADMIN PORTAL                     ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] View All Customer Accounts & Balances                       ║");
            System.out.println("║  [2] Unlock / Reset Locked Customer Account                      ║");
            System.out.println("║  [3] Register New Customer Account                               ║");
            System.out.println("║  [4] ATM Cash Vault Status & Replenishment                       ║");
            System.out.println("║  [5] Bank Summary & System Analytics                             ║");
            System.out.println("║  [6] Exit Admin Portal                                           ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝" + ConsoleUI.RESET);
            System.out.print(ConsoleUI.YELLOW + "Select admin option (1-6): " + ConsoleUI.RESET);

            String opt = scanner.nextLine().trim();
            switch (opt) {
                case "1" -> showAllAccountsAdmin();
                case "2" -> handleUnlockAccountAdmin();
                case "3" -> handleAccountRegistration();
                case "4" -> handleVaultAdmin();
                case "5" -> handleBankAnalyticsAdmin();
                case "6" -> {
                    adminActive = false;
                    ConsoleUI.printInfo("Exited Admin Portal.");
                }
                default -> ConsoleUI.printError("Invalid choice.");
            }
        }
    }

    private void showAllAccountsAdmin() {
        ConsoleUI.printHeader("Registered Bank Accounts");
        Collection<Account> accounts = bank.getAllAccounts();
        System.out.printf("%-10s | %-22s | %-10s | %-10s | %12s | %12s%n",
                "ACC ID", "OWNER NAME", "TYPE", "STATUS", "BALANCE", "LIMIT REM");
        System.out.println("─".repeat(88));
        for (Account a : accounts) {
            String statusColor = a.isLocked() ? ConsoleUI.RED : ConsoleUI.GREEN;
            System.out.printf("%-10s | %-22s | %-10s | " + statusColor + "%-10s" + ConsoleUI.RESET + " | ₹%11.2f | ₹%11.2f%n",
                    a.getAccountId(),
                    truncate(a.getOwnerName(), 22),
                    a.getAccountType(),
                    a.getStatus(),
                    a.getBalance(),
                    a.getRemainingDailyLimit());
        }
    }

    private void handleUnlockAccountAdmin() {
        ConsoleUI.printHeader("Unlock Customer Account");
        System.out.print("Enter Account ID to unlock: ");
        String id = scanner.nextLine().trim();
        Account acc = bank.findAccount(id);
        if (acc == null) {
            ConsoleUI.printError("Account not found.");
            return;
        }

        if (!acc.isLocked()) {
            ConsoleUI.printInfo("Account " + id + " is already ACTIVE.");
            return;
        }

        boolean ok = bank.unlockAccount(id);
        if (ok) {
            ConsoleUI.printSuccess("Account " + id + " (" + acc.getOwnerName() + ") has been unlocked successfully.");
        } else {
            ConsoleUI.printError("Could not unlock account.");
        }
    }

    private void handleVaultAdmin() {
        CashVault vault = bank.getCashVault();
        ConsoleUI.printHeader("ATM Cash Vault Inventory");
        System.out.println("  ₹500 Notes:  " + vault.getCount500() + " notes (₹" + (vault.getCount500() * 500) + ")");
        System.out.println("  ₹200 Notes:  " + vault.getCount200() + " notes (₹" + (vault.getCount200() * 200) + ")");
        System.out.println("  ₹100 Notes:  " + vault.getCount100() + " notes (₹" + (vault.getCount100() * 100) + ")");
        System.out.println(ConsoleUI.GREEN + BOLD + String.format("  Total Physical Cash in ATM: ₹%.2f", vault.getTotalCash()) + ConsoleUI.RESET);

        System.out.print(ConsoleUI.YELLOW + "\nWould you like to replenish/restock vault cash? (Y/N): " + ConsoleUI.RESET);
        String ans = scanner.nextLine().trim();
        if ("Y".equalsIgnoreCase(ans) || "YES".equalsIgnoreCase(ans)) {
            System.out.print("Enter additional ₹500 notes to add: ");
            int add500 = promptInt();
            System.out.print("Enter additional ₹200 notes to add: ");
            int add200 = promptInt();
            System.out.print("Enter additional ₹100 notes to add: ");
            int add100 = promptInt();

            vault.depositNotes(add500, add200, add100);
            bank.save();
            ConsoleUI.printSuccess(String.format("Vault restocked! New Total: ₹%.2f", vault.getTotalCash()));
        }
    }

    private void handleBankAnalyticsAdmin() {
        ConsoleUI.printHeader("Bank System Analytics");
        Collection<Account> all = bank.getAllAccounts();
        double totalDeposits = bank.getTotalBankDeposits();
        int totalTxns = bank.getTotalTransactionsCount();
        long lockedCount = all.stream().filter(Account::isLocked).count();

        System.out.println("  Total Registered Accounts:  " + all.size());
        System.out.println("  Active Accounts:            " + (all.size() - lockedCount));
        System.out.println("  Locked Accounts:            " + (lockedCount > 0 ? ConsoleUI.RED : ConsoleUI.GREEN) + lockedCount + ConsoleUI.RESET);
        System.out.printf("  Total Customer Deposits:    ₹%.2f%n", totalDeposits);
        System.out.printf("  ATM Physical Cash on Hand:  ₹%.2f%n", bank.getCashVault().getTotalCash());
        System.out.println("  Total Transactions Logged:  " + totalTxns);
        ConsoleUI.printDivider();
    }

    // ==========================================
    // UTILITY HELPERS
    // ==========================================

    private Double promptAmount(String prompt) {
        System.out.print(ConsoleUI.YELLOW + prompt + ConsoleUI.RESET);
        String input = scanner.nextLine().trim();
        try {
            double amt = Double.parseDouble(input);
            if (amt <= 0) {
                ConsoleUI.printError("Amount must be greater than zero.");
                return null;
            }
            return amt;
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid numerical format.");
            return null;
        }
    }

    private int promptInt() {
        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return 0;
            int v = Integer.parseInt(input);
            return Math.max(0, v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final String BOLD = ConsoleUI.BOLD;

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }
}
