import java.io.File;
import java.util.Map;

/**
 * Automated Test Suite for the Java ATM Project.
 * Validates domain models, business rules, cash vault denomination algorithms,
 * security lockouts, transfers, and persistence.
 */
public class TestRunner {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     RUNNING ATM AUTOMATED TEST SUITE             ");
        System.out.println("==================================================");

        testAuthenticationAndLockout();
        testCashVaultDispenser();
        testDepositAndWithdrawal();
        testDailyLimits();
        testInterAccountTransfer();
        testPinChange();
        testStoragePersistence();
        testAdminOperations();

        System.out.println("\n==================================================");
        System.out.printf("TEST RESULTS: %d Passed, %d Failed%n", testsPassed, testsFailed);
        System.out.println("==================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertEquals(Object expected, Object actual, String testName) {
        if (expected == null && actual == null) {
            pass(testName);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            pass(testName);
        } else {
            fail(testName, "Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            pass(testName);
        } else {
            fail(testName, "Expected true, got false");
        }
    }

    private static void assertFalse(boolean condition, String testName) {
        if (!condition) {
            pass(testName);
        } else {
            fail(testName, "Expected false, got true");
        }
    }

    private static void pass(String testName) {
        testsPassed++;
        System.out.println("  ✔ [PASS] " + testName);
    }

    private static void fail(String testName, String reason) {
        testsFailed++;
        System.out.println("  ✘ [FAIL] " + testName + " -> " + reason);
    }

    // ==========================================
    // TESTS
    // ==========================================

    private static void testAuthenticationAndLockout() {
        System.out.println("\n--- Testing Authentication & Account Lockout ---");
        File tempFile = new File("data/test_auth.json");
        tempFile.deleteOnExit();
        Bank bank = new Bank(new StorageManager(tempFile), new CashVault());

        // Valid login
        Bank.AuthResult r1 = bank.authenticate("1001", "1234");
        assertTrue(r1.isSuccess(), "Login with correct credentials succeeds");
        assertEquals("Manjari PA", r1.getAccount().getOwnerName(), "Authenticated account owner matches");

        // Wrong PIN
        Bank.AuthResult r2 = bank.authenticate("1001", "0000");
        assertFalse(r2.isSuccess(), "Login with wrong PIN fails");
        assertEquals(2, r2.getRemainingAttempts(), "Attempts remaining decrements to 2");

        // Second failure
        Bank.AuthResult r3 = bank.authenticate("1001", "0000");
        assertFalse(r3.isSuccess(), "Second wrong PIN fails");
        assertEquals(1, r3.getRemainingAttempts(), "Attempts remaining decrements to 1");

        // Third failure -> Locks account
        Bank.AuthResult r4 = bank.authenticate("1001", "0000");
        assertFalse(r4.isSuccess(), "Third wrong PIN fails");
        assertTrue(r4.isAccountLocked(), "Account is locked after 3 failures");

        // Attempting to login when locked with correct PIN
        Bank.AuthResult r5 = bank.authenticate("1001", "1234");
        assertFalse(r5.isSuccess(), "Locked account cannot authenticate even with correct PIN");
        assertTrue(r5.isAccountLocked(), "Locked account reports locked status");

        tempFile.delete();
    }

    private static void testCashVaultDispenser() {
        System.out.println("\n--- Testing Cash Vault Denominations Dispenser ---");
        CashVault vault = new CashVault(10, 10, 10); // 10x500 (5000) + 10x200 (2000) + 10x100 (1000) = 8000
        assertEquals(8000.0, vault.getTotalCash(), "Initial total cash is 8000");

        // Test non-multiples of 100
        assertFalse(vault.canDispense(250), "Cannot dispense non-multiple of 100");
        assertFalse(vault.canDispense(-100), "Cannot dispense negative amount");
        assertFalse(vault.canDispense(9000), "Cannot dispense more than vault cash");

        // Test denomination calculation for ₹700 -> 1x500 + 1x200
        Map<Integer, Integer> plan700 = vault.calculateDispensePlan(700);
        assertEquals(1, (int) plan700.get(500), "700 breakdown has 1x500");
        assertEquals(1, (int) plan700.get(200), "700 breakdown has 1x200");

        // Test denomination calculation for ₹1300 -> 2x500 + 1x200 + 1x100
        Map<Integer, Integer> plan1300 = vault.calculateDispensePlan(1300);
        assertEquals(2, (int) plan1300.get(500), "1300 breakdown has 2x500");
        assertEquals(1, (int) plan1300.get(200), "1300 breakdown has 1x200");
        assertEquals(1, (int) plan1300.get(100), "1300 breakdown has 1x100");

        // Dispense ₹800
        Map<Integer, Integer> dispensed = vault.dispenseNotes(800);
        assertEquals(7200.0, vault.getTotalCash(), "Vault cash is 7200 after dispensing 800");

        // Deposit notes
        vault.depositNotes(2, 0, 0); // + 1000
        assertEquals(8200.0, vault.getTotalCash(), "Vault cash is 8200 after depositing 2x500");
    }

    private static void testDepositAndWithdrawal() {
        System.out.println("\n--- Testing Deposits and Withdrawals ---");
        Account acc = new Account("9999", "Test User", "1111", 1000.0);

        // Deposit
        Transaction tDep = acc.deposit(500.0, "Test Deposit");
        assertEquals(1500.0, acc.getBalance(), "Balance after deposit is 1500");
        assertEquals(Transaction.Type.DEPOSIT, tDep.getType(), "Deposit transaction type is DEPOSIT");

        // Withdrawal
        Transaction tWith = acc.withdraw(300.0, "Test Withdrawal");
        assertEquals(1200.0, acc.getBalance(), "Balance after withdrawal is 1200");
        assertEquals(Transaction.Type.WITHDRAWAL, tWith.getType(), "Withdrawal transaction type is WITHDRAWAL");

        // Overdraft withdrawal should fail
        Transaction tOverdraft = acc.withdraw(2000.0, "Overdraft");
        assertEquals(null, tOverdraft, "Overdraft withdrawal returns null");
        assertEquals(1200.0, acc.getBalance(), "Balance unchanged after failed withdrawal");
    }

    private static void testDailyLimits() {
        System.out.println("\n--- Testing Daily Limits ---");
        Account acc = new Account("9998", "Limit User", "1111", 50000.0);
        acc.setDailyWithdrawalLimit(5000.0);

        assertEquals(5000.0, acc.getRemainingDailyLimit(), "Initial remaining daily limit is 5000");

        // Withdraw within limit
        acc.withdraw(3000.0, "Withdrawal 1");
        assertEquals(2000.0, acc.getRemainingDailyLimit(), "Remaining limit is 2000 after 3000 withdrawal");

        // Withdraw exceeding remaining limit
        Transaction tFail = acc.withdraw(2500.0, "Exceed Limit");
        assertEquals(null, tFail, "Withdrawal exceeding daily limit fails");
        assertEquals(2000.0, acc.getRemainingDailyLimit(), "Remaining limit remains 2000");

        // Withdraw exact remaining
        Transaction tOk = acc.withdraw(2000.0, "Withdraw Remaining");
        assertTrue(tOk != null, "Withdrawal of remaining limit succeeds");
        assertEquals(0.0, acc.getRemainingDailyLimit(), "Remaining limit is 0");
    }

    private static void testInterAccountTransfer() {
        System.out.println("\n--- Testing Inter-Account Transfers ---");
        File tempFile = new File("data/test_transfer.json");
        tempFile.deleteOnExit();
        Bank bank = new Bank(new StorageManager(tempFile), new CashVault());

        Account from = bank.findAccount("1001"); // 5000
        Account to = bank.findAccount("1002");   // 12500.50

        // Self transfer rejected
        boolean selfTransfer = bank.transfer(from, "1001", 500.0);
        assertFalse(selfTransfer, "Self-transfer is rejected");

        // Transfer to non-existent account rejected
        boolean invalidRecipient = bank.transfer(from, "9999", 500.0);
        assertFalse(invalidRecipient, "Transfer to non-existent account is rejected");

        // Valid transfer of ₹1000
        boolean ok = bank.transfer(from, "1002", 1000.0);
        assertTrue(ok, "Valid transfer succeeds");
        assertEquals(4000.0, from.getBalance(), "Sender balance reduced by 1000");
        assertEquals(13500.50, to.getBalance(), "Recipient balance increased by 1000");

        // Insufficient funds transfer rejected
        boolean overTransfer = bank.transfer(from, "1002", 10000.0);
        assertFalse(overTransfer, "Transfer with insufficient funds is rejected");

        tempFile.delete();
    }

    private static void testPinChange() {
        System.out.println("\n--- Testing PIN Change ---");
        Account acc = new Account("9997", "Pin User", "1234", 1000.0);

        // Wrong old PIN
        assertFalse(acc.changePin("9999", "5678"), "Change PIN fails with wrong old PIN");

        // Invalid new PIN format (letters)
        assertFalse(acc.changePin("1234", "abcd"), "Change PIN fails with non-digits");

        // Invalid new PIN format (short)
        assertFalse(acc.changePin("1234", "12"), "Change PIN fails with less than 4 digits");

        // Same PIN
        assertFalse(acc.changePin("1234", "1234"), "Change PIN fails when new PIN equals old PIN");

        // Valid PIN change
        assertTrue(acc.changePin("1234", "5678"), "Change PIN succeeds with valid input");
        assertTrue(acc.checkPin("5678"), "New PIN is verified");
        assertFalse(acc.checkPin("1234"), "Old PIN no longer works");
    }

    private static void testStoragePersistence() {
        System.out.println("\n--- Testing Storage Persistence ---");
        File storageFile = new File("data/test_persistence.json");
        storageFile.deleteOnExit();

        StorageManager storage = new StorageManager(storageFile);
        CashVault vault1 = new CashVault(20, 30, 40);
        Bank bank1 = new Bank(storage, vault1);

        // Perform some operations
        Account acc = bank1.findAccount("1001");
        acc.deposit(1234.0, "Special Deposit");
        acc.withdraw(500.0, "Special Withdrawal");
        bank1.createAccount("8888", "New Customer", "9876", 2500.0, Account.AccountType.CURRENT);
        bank1.save();

        // Reload in brand new bank instance
        CashVault vault2 = new CashVault(0, 0, 0);
        Bank bank2 = new Bank(storage, vault2);

        Account reloadedAcc = bank2.findAccount("1001");
        assertTrue(reloadedAcc != null, "Reloaded account 1001 exists");
        assertEquals(5734.0, reloadedAcc.getBalance(), "Reloaded balance matches after deposit & withdrawal");

        Account newAcc = bank2.findAccount("8888");
        assertTrue(newAcc != null, "Reloaded newly created account 8888 exists");
        assertEquals(2500.0, newAcc.getBalance(), "Reloaded newly created account balance matches");

        assertEquals(vault1.getTotalCash(), vault2.getTotalCash(), "Reloaded cash vault total cash matches");

        storageFile.delete();
    }

    private static void testAdminOperations() {
        System.out.println("\n--- Testing Admin Operations ---");
        File tempFile = new File("data/test_admin.json");
        tempFile.deleteOnExit();
        Bank bank = new Bank(new StorageManager(tempFile), new CashVault());

        // Admin Auth
        assertTrue(bank.authenticateAdmin(Bank.ADMIN_ID, Bank.ADMIN_PIN), "Admin authentication succeeds");
        assertFalse(bank.authenticateAdmin("wrong", "0000"), "Admin authentication fails with wrong creds");

        // Lock and Admin Unlock
        Account acc = bank.findAccount("1001");
        acc.lock();
        assertTrue(acc.isLocked(), "Account locked");
        bank.unlockAccount("1001");
        assertFalse(acc.isLocked(), "Admin successfully unlocked account");

        // Total Bank Deposits
        double total = bank.getTotalBankDeposits();
        assertTrue(total > 0, "Total bank deposits calculated correctly: ₹" + total);

        tempFile.delete();
    }
}
