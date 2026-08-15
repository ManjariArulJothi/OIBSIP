/**
 * Application entry point for the Enhanced Java Console ATM.
 * Initializes the Bank (with sample demo accounts and persistent storage)
 * and starts the ATM interface.
 *
 * Demo Customer Credentials:
 *   User ID: 1001   PIN: 1234   (Manjari PA       - Balance: ₹5,000.00)
 *   User ID: 1002   PIN: 4321   (Arjun Kumar     - Balance: ₹12,500.50)
 *   User ID: 1003   PIN: 1111   (Divya Sree      - Balance: ₹750.00)
 *   User ID: 1004   PIN: 5555   (TechCorp Ent    - Balance: ₹85,000.00)
 *
 * Administrator Credentials:
 *   Admin ID: admin  Passcode: 9999
 */
public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        ATM atm = new ATM(bank);
        atm.start();
    }
}
