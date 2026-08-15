import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulates the physical ATM cash dispenser and vault.
 * Manages currency note inventory (₹500, ₹200, ₹100) and computes
 * optimal note denomination breakdowns during withdrawals.
 */
public class CashVault {

    private int count500;
    private int count200;
    private int count100;

    public CashVault() {
        // Default initial stock: 50x500 (25k), 50x200 (10k), 100x100 (10k) = ₹45,000
        this(50, 50, 100);
    }

    public CashVault(int count500, int count200, int count100) {
        this.count500 = count500;
        this.count200 = count200;
        this.count100 = count100;
    }

    public synchronized double getTotalCash() {
        return (count500 * 500.0) + (count200 * 200.0) + (count100 * 100.0);
    }

    public int getCount500() {
        return count500;
    }

    public int getCount200() {
        return count200;
    }

    public int getCount100() {
        return count100;
    }

    /**
     * Checks if the ATM vault can fulfill dispensing the given amount
     * based on available note denominations.
     */
    public synchronized boolean canDispense(double amount) {
        if (amount <= 0 || amount % 100 != 0 || amount > getTotalCash()) {
            return false;
        }
        return calculateDispensePlan((int) amount) != null;
    }

    /**
     * Calculates the note breakdown for a given amount without mutating vault state.
     * Returns a map of denomination -> count, or null if impossible.
     */
    public synchronized Map<Integer, Integer> calculateDispensePlan(int amount) {
        if (amount <= 0 || amount % 100 != 0) {
            return null;
        }

        int remaining = amount;
        int use500 = 0;
        int use200 = 0;
        int use100 = 0;

        // Try greedy strategy with backtracking for 200/100 combinations
        int max500 = Math.min(remaining / 500, count500);
        for (int c500 = max500; c500 >= 0; c500--) {
            int remAfter500 = remaining - (c500 * 500);
            int max200 = Math.min(remAfter500 / 200, count200);
            for (int c200 = max200; c200 >= 0; c200--) {
                int remAfter200 = remAfter500 - (c200 * 200);
                if (remAfter200 % 100 == 0) {
                    int c100 = remAfter200 / 100;
                    if (c100 <= count100) {
                        use500 = c500;
                        use200 = c200;
                        use100 = c100;
                        Map<Integer, Integer> plan = new LinkedHashMap<>();
                        if (use500 > 0) plan.put(500, use500);
                        if (use200 > 0) plan.put(200, use200);
                        if (use100 > 0) plan.put(100, use100);
                        return plan;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Dispenses the requested amount and deducts notes from the vault.
     * Returns note breakdown map if successful, or null if failed.
     */
    public synchronized Map<Integer, Integer> dispenseNotes(double amount) {
        Map<Integer, Integer> plan = calculateDispensePlan((int) amount);
        if (plan == null) {
            return null;
        }

        count500 -= plan.getOrDefault(500, 0);
        count200 -= plan.getOrDefault(200, 0);
        count100 -= plan.getOrDefault(100, 0);
        return plan;
    }

    /**
     * Adds deposited currency notes to the ATM vault.
     */
    public synchronized void depositNotes(int n500, int n200, int n100) {
        if (n500 > 0) count500 += n500;
        if (n200 > 0) count200 += n200;
        if (n100 > 0) count100 += n100;
    }

    /**
     * Restocks the vault with specific quantities of notes.
     */
    public synchronized void restock(int n500, int n200, int n100) {
        this.count500 = Math.max(0, n500);
        this.count200 = Math.max(0, n200);
        this.count100 = Math.max(0, n100);
    }

    /**
     * Converts note breakdown map to a friendly string format (e.g. "2x₹500, 1x₹200").
     */
    public static String formatNoteBreakdown(Map<Integer, Integer> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<Integer, Integer> entry : notes.entrySet()) {
            if (count > 0) sb.append(", ");
            sb.append(entry.getValue()).append("x₹").append(entry.getKey());
            count++;
        }
        return sb.toString();
    }
}
