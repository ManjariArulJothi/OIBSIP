import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single transaction performed on a bank account.
 * Immutable record capturing type, amount, post-transaction balance,
 * description, notes dispensed/deposited, and timestamp.
 */
public class Transaction {

    private static int nextId = 10001;

    public enum Type {
        DEPOSIT,
        WITHDRAWAL,
        FAST_CASH,
        TRANSFER_OUT,
        TRANSFER_IN,
        PIN_CHANGE
    }

    private final String id;
    private final Type type;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final String noteBreakdown;
    private final LocalDateTime timestamp;

    public Transaction(Type type, double amount, double balanceAfter, String description) {
        this(type, amount, balanceAfter, description, null);
    }

    public Transaction(Type type, double amount, double balanceAfter, String description, String noteBreakdown) {
        this("TXN-" + (nextId++), type, amount, balanceAfter, description, noteBreakdown, LocalDateTime.now());
    }

    public Transaction(String id, Type type, double amount, double balanceAfter, String description, String noteBreakdown, LocalDateTime timestamp) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.noteBreakdown = noteBreakdown;
        this.timestamp = timestamp;

        // Keep running counter ahead of any loaded transaction ID
        try {
            if (id.startsWith("TXN-")) {
                int numericId = Integer.parseInt(id.substring(4));
                if (numericId >= nextId) {
                    nextId = numericId + 1;
                }
            }
        } catch (NumberFormatException ignored) {}
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public String getNoteBreakdown() {
        return noteBreakdown;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return timestamp.format(fmt);
    }

    @Override
    public String toString() {
        return String.format(
                "%-10s | %-19s | %-12s | ₹%10.2f | ₹%10.2f | %s%s",
                id,
                getFormattedTimestamp(),
                type,
                amount,
                balanceAfter,
                description,
                (noteBreakdown != null && !noteBreakdown.isEmpty() ? " [" + noteBreakdown + "]" : "")
        );
    }
}
