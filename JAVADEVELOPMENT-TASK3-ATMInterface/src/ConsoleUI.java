import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

/**
 * Terminal presentation utility providing ANSI colored output, styled ASCII banners,
 * formatted tables, interactive prompt helpers, and receipt printing/saving capabilities.
 */
public class ConsoleUI {

    // ANSI Escape Codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static void printBanner() {
        System.out.println(CYAN + BOLD + "╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                        ║");
        System.out.println("║    ███████╗ ███████╗  ██████╗  ██████╗  █████╗ ███╗   ██╗██╗  ██╗      ║");
        System.out.println("║    ██╔════╝ ██╔════╝ ██╔═══██╗██╔════╝ ██╔══██╗████╗  ██║██║ ██╔╝      ║");
        System.out.println("║    █████╗   ███████╗██║   ██║██║  ███╗███████║██╔██╗ ██║█████═╝       ║");
        System.out.println("║    ██╔══╝   ╚════██║██║   ██║██║   ██║██╔══██║██║╚██╗██║██╔═██╗       ║");
        System.out.println("║    ███████╗ ███████║╚██████╔╝╚██████╔╝██║  ██║██║ ╚████║██║ ╚██╗      ║");
        System.out.println("║    ╚══════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝      ║");
        System.out.println("║                                                                        ║");
        System.out.println("║              ★ NEXT-GEN SECURE AUTOMATED TELLER MACHINE ★              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝" + RESET);
    }

    public static void printHeader(String title) {
        System.out.println("\n" + CYAN + BOLD + "▶ " + title.toUpperCase() + RESET);
        System.out.println(CYAN + "─".repeat(Math.max(40, title.length() + 4)) + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + BOLD + "✔ SUCCESS: " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + BOLD + "✘ ERROR: " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + BOLD + "⚠ NOTICE: " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "ℹ " + message + RESET);
    }

    public static void printDivider() {
        System.out.println(CYAN + "────────────────────────────────────────────────────────────────────────" + RESET);
    }

    /**
     * Reads a PIN securely if interactive console is present, otherwise falls back to Scanner.
     */
    public static String promptPin(Scanner scanner, String promptText) {
        System.out.print(YELLOW + promptText + RESET);
        if (System.console() != null) {
            char[] chars = System.console().readPassword();
            if (chars != null) {
                return new String(chars).trim();
            }
        }
        return scanner.nextLine().trim();
    }

    /**
     * Prints transaction history table.
     */
    public static void printTransactionTable(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println(YELLOW + "  No transactions recorded for this account." + RESET);
            return;
        }

        System.out.println(BOLD + String.format(
                "%-12s | %-19s | %-13s | %11s | %11s | %s",
                "TXN ID", "DATE & TIME", "TYPE", "AMOUNT", "BALANCE", "DETAILS"
        ) + RESET);
        System.out.println("─".repeat(95));

        for (Transaction t : transactions) {
            String color = switch (t.getType()) {
                case DEPOSIT, TRANSFER_IN -> GREEN;
                case WITHDRAWAL, FAST_CASH, TRANSFER_OUT -> RED;
                case PIN_CHANGE -> YELLOW;
            };

            String notesStr = (t.getNoteBreakdown() != null && !t.getNoteBreakdown().isEmpty())
                    ? " (" + t.getNoteBreakdown() + ")" : "";

            System.out.printf(
                    "%-12s | %-19s | " + color + "%-13s" + RESET + " | " + color + "₹%10.2f" + RESET + " | ₹%10.2f | %s%s%n",
                    t.getId(),
                    t.getFormattedTimestamp(),
                    t.getType(),
                    t.getAmount(),
                    t.getBalanceAfter(),
                    t.getDescription(),
                    notesStr
            );
        }
    }

    /**
     * Formats and prints an official transaction receipt on screen and saves to a file.
     */
    public static void printAndSaveReceipt(Account account, Transaction txn) {
        String receipt = formatReceipt(account, txn);
        System.out.println("\n" + receipt);

        // Save to receipts/ folder
        try {
            File receiptsDir = new File("receipts");
            if (!receiptsDir.exists()) {
                receiptsDir.mkdirs();
            }
            String fileName = String.format("receipts/receipt_%s.txt", txn.getId());
            File file = new File(fileName);
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                osw.write(stripAnsi(receipt));
            }
            printInfo("A physical receipt copy has been generated: " + fileName);
        } catch (Exception e) {
            // Silently continue if file write fails
        }
    }

    private static String formatReceipt(Account account, Transaction txn) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm:ss a"));
        String maskedAcc = maskAccountId(account.getAccountId());
        String notes = txn.getNoteBreakdown() != null && !txn.getNoteBreakdown().isEmpty()
                ? "\n  Dispensed Notes: " + txn.getNoteBreakdown() : "";

        return """
        ┌──────────────────────────────────────────────────┐
        │               ESGOKAN REGIONAL BANK              │
        │               ATM TRANSACTION RECEIPT            │
        ├──────────────────────────────────────────────────┤
        │ Terminal: ATM-BLR-042       Branch: Central HQ   │
        │ Date/Time: %-37s │
        │ Txn ID:    %-37s │
        ├──────────────────────────────────────────────────┤
        │ Account No: %-36s │
        │ Card Holder: %-35s │
        │ Account Type: %-34s │
        ├──────────────────────────────────────────────────┤
        │ Transaction:  %-34s │
        │ Amount:       ₹%-33.2f │
        │ Available Bal:₹%-33.2f │%s
        ├──────────────────────────────────────────────────┤
        │ Thank you for banking with us!                   │
        │ 24x7 Customer Helpline: 1800-ESG-BANK            │
        └──────────────────────────────────────────────────┘
        """.formatted(
                now,
                txn.getId(),
                maskedAcc,
                account.getOwnerName(),
                account.getAccountType(),
                txn.getType(),
                txn.getAmount(),
                account.getBalance(),
                notes
        );
    }

    private static String maskAccountId(String accountId) {
        if (accountId.length() <= 2) return accountId;
        return "XXXX-" + accountId.substring(Math.max(0, accountId.length() - 2));
    }

    private static String stripAnsi(String str) {
        return str.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
