import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles persistent storage of bank accounts, customer transaction records,
 * and ATM cash vault inventory. Uses standard Java I/O to read and write
 * a structured JSON storage format in the `data/` directory.
 */
public class StorageManager {

    private final File dataFile;

    public StorageManager() {
        this(new File("data/atm_data.json"));
    }

    public StorageManager(File dataFile) {
        this.dataFile = dataFile;
    }

    public synchronized boolean hasPersistedData() {
        return dataFile.exists() && dataFile.length() > 0;
    }

    /**
     * Saves the entire bank and ATM state to file.
     */
    public synchronized boolean save(Map<String, Account> accounts, CashVault vault) {
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");

            // 1. Cash Vault section
            json.append("  \"vault\": {\n");
            json.append(String.format("    \"count500\": %d,\n", vault.getCount500()));
            json.append(String.format("    \"count200\": %d,\n", vault.getCount200()));
            json.append(String.format("    \"count100\": %d\n", vault.getCount100()));
            json.append("  },\n");

            // 2. Accounts section
            json.append("  \"accounts\": [\n");
            int accIndex = 0;
            for (Account acc : accounts.values()) {
                json.append("    {\n");
                json.append(String.format("      \"accountId\": \"%s\",\n", escapeJson(acc.getAccountId())));
                json.append(String.format("      \"ownerName\": \"%s\",\n", escapeJson(acc.getOwnerName())));
                json.append(String.format("      \"pin\": \"%s\",\n", escapeJson(acc.getPin())));
                json.append(String.format("      \"balance\": %.2f,\n", acc.getBalance()));
                json.append(String.format("      \"accountType\": \"%s\",\n", acc.getAccountType().name()));
                json.append(String.format("      \"status\": \"%s\",\n", acc.getStatus().name()));
                json.append(String.format("      \"dailyLimit\": %.2f,\n", acc.getDailyWithdrawalLimit()));
                json.append(String.format("      \"dailyWithdrawn\": %.2f,\n", acc.getDailyWithdrawnToday()));
                json.append(String.format("      \"lastWithdrawalDate\": \"%s\",\n", acc.getLastWithdrawalDate() != null ? acc.getLastWithdrawalDate() : LocalDate.now()));

                // History
                json.append("      \"history\": [\n");
                List<Transaction> txns = acc.getHistory();
                for (int i = 0; i < txns.size(); i++) {
                    Transaction t = txns.get(i);
                    json.append("        {\n");
                    json.append(String.format("          \"id\": \"%s\",\n", escapeJson(t.getId())));
                    json.append(String.format("          \"type\": \"%s\",\n", t.getType().name()));
                    json.append(String.format("          \"amount\": %.2f,\n", t.getAmount()));
                    json.append(String.format("          \"balanceAfter\": %.2f,\n", t.getBalanceAfter()));
                    json.append(String.format("          \"description\": \"%s\",\n", escapeJson(t.getDescription())));
                    json.append(String.format("          \"noteBreakdown\": \"%s\",\n", escapeJson(t.getNoteBreakdown() != null ? t.getNoteBreakdown() : "")));
                    json.append(String.format("          \"timestamp\": \"%s\"\n", t.getTimestamp()));
                    json.append("        }").append(i < txns.size() - 1 ? "," : "").append("\n");
                }
                json.append("      ]\n");

                json.append("    }").append(accIndex < accounts.size() - 1 ? "," : "").append("\n");
                accIndex++;
            }
            json.append("  ]\n");
            json.append("}\n");

            try (FileOutputStream fos = new FileOutputStream(dataFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(json.toString());
            }
            return true;
        } catch (Exception e) {
            System.err.println("Warning: Failed to persist bank state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads bank and vault state from disk.
     */
    public synchronized Map<String, Account> load(CashVault vault) {
        if (!hasPersistedData()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(dataFile);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }

            return parseJsonData(content.toString(), vault);
        } catch (Exception e) {
            System.err.println("Warning: Failed to load bank state from file: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Account> parseJsonData(String rawJson, CashVault vault) {
        Map<String, Account> accounts = new HashMap<>();

        // Extract Vault
        int vaultStart = rawJson.indexOf("\"vault\":");
        if (vaultStart != -1 && vault != null) {
            int c500 = extractInt(rawJson, "count500", 50);
            int c200 = extractInt(rawJson, "count200", 50);
            int c100 = extractInt(rawJson, "count100", 100);
            vault.restock(c500, c200, c100);
        }

        // Extract Accounts
        int accSection = rawJson.indexOf("\"accounts\":");
        if (accSection == -1) {
            return accounts;
        }

        String accountsPart = rawJson.substring(accSection);
        // Find individual account blocks
        int cursor = 0;
        while ((cursor = accountsPart.indexOf("\"accountId\":", cursor)) != -1) {
            int blockStart = accountsPart.lastIndexOf("{", cursor);
            // find matching closing brace for account block
            int blockEnd = findAccountBlockEnd(accountsPart, blockStart);
            if (blockEnd == -1) break;

            String accBlock = accountsPart.substring(blockStart, blockEnd);

            String accId = extractString(accBlock, "accountId");
            String owner = extractString(accBlock, "ownerName");
            String pin = extractString(accBlock, "pin");
            double balance = extractDouble(accBlock, "balance", 0.0);
            String typeStr = extractString(accBlock, "accountType");
            String statusStr = extractString(accBlock, "status");
            double dailyLimit = extractDouble(accBlock, "dailyLimit", 20000.0);
            double dailyWithdrawn = extractDouble(accBlock, "dailyWithdrawn", 0.0);
            String dateStr = extractString(accBlock, "lastWithdrawalDate");

            Account.AccountType accType = "CURRENT".equalsIgnoreCase(typeStr) ? Account.AccountType.CURRENT : Account.AccountType.SAVINGS;
            Account.Status status = "LOCKED".equalsIgnoreCase(statusStr) ? Account.Status.LOCKED :
                    ("SUSPENDED".equalsIgnoreCase(statusStr) ? Account.Status.SUSPENDED : Account.Status.ACTIVE);
            LocalDate lastDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

            Account account = new Account(accId, owner, pin, balance, accType, status, dailyLimit, dailyWithdrawn, lastDate);

            // Parse history
            int histStart = accBlock.indexOf("\"history\":");
            if (histStart != -1) {
                String histPart = accBlock.substring(histStart);
                int hCursor = 0;
                while ((hCursor = histPart.indexOf("\"id\":", hCursor)) != -1) {
                    int tStart = histPart.lastIndexOf("{", hCursor);
                    int tEnd = histPart.indexOf("}", tStart);
                    if (tEnd == -1) break;

                    String tBlock = histPart.substring(tStart, tEnd + 1);
                    String tId = extractString(tBlock, "id");
                    String tTypeStr = extractString(tBlock, "type");
                    double tAmt = extractDouble(tBlock, "amount", 0.0);
                    double tBalAfter = extractDouble(tBlock, "balanceAfter", 0.0);
                    String tDesc = extractString(tBlock, "description");
                    String tNotes = extractString(tBlock, "noteBreakdown");
                    String tTimeStr = extractString(tBlock, "timestamp");

                    Transaction.Type tType;
                    try {
                        tType = Transaction.Type.valueOf(tTypeStr);
                    } catch (Exception e) {
                        tType = Transaction.Type.DEPOSIT;
                    }

                    LocalDateTime tTime = (tTimeStr != null && !tTimeStr.isEmpty()) ? LocalDateTime.parse(tTimeStr) : LocalDateTime.now();
                    Transaction txn = new Transaction(tId, tType, tAmt, tBalAfter, tDesc, tNotes, tTime);
                    account.addHistoricalTransaction(txn);

                    hCursor = tEnd + 1;
                }
            }

            accounts.put(accId, account);
            cursor = blockEnd;
        }

        return accounts;
    }

    private int findAccountBlockEnd(String text, int start) {
        int braceDepth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private String extractString(String block, String key) {
        String marker = "\"" + key + "\":";
        int idx = block.indexOf(marker);
        if (idx == -1) return "";
        int valStart = block.indexOf("\"", idx + marker.length());
        if (valStart == -1) return "";
        int valEnd = block.indexOf("\"", valStart + 1);
        if (valEnd == -1) return "";
        return block.substring(valStart + 1, valEnd);
    }

    private int extractInt(String block, String key, int defaultVal) {
        try {
            String marker = "\"" + key + "\":";
            int idx = block.indexOf(marker);
            if (idx == -1) return defaultVal;
            int numStart = idx + marker.length();
            int numEnd = indexOfAny(block, new char[]{',', '\n', '}', '\r'}, numStart);
            if (numEnd == -1) numEnd = block.length();
            return Integer.parseInt(block.substring(numStart, numEnd).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private double extractDouble(String block, String key, double defaultVal) {
        try {
            String marker = "\"" + key + "\":";
            int idx = block.indexOf(marker);
            if (idx == -1) return defaultVal;
            int numStart = idx + marker.length();
            int numEnd = indexOfAny(block, new char[]{',', '\n', '}', '\r'}, numStart);
            if (numEnd == -1) numEnd = block.length();
            return Double.parseDouble(block.substring(numStart, numEnd).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private int indexOfAny(String str, char[] chars, int fromIndex) {
        for (int i = fromIndex; i < str.length(); i++) {
            char c = str.charAt(i);
            for (char target : chars) {
                if (c == target) return i;
            }
        }
        return -1;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
