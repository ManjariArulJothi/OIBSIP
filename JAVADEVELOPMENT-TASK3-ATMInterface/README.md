# 🏧 Enhanced Java Console ATM & Banking Simulator

A feature-complete, modern, and production-grade console ATM banking simulation written in clean, dependency-free Java (JDK 17+).

---

## 🌟 Key Features

### 💳 1. Customer Banking Services
- **Secure Authentication & Account Lockout**:
  - Account ID + Security PIN verification.
  - Automatic security lockout after **3 consecutive failed PIN attempts** (persisted across sessions).
- **Balance Inquiry**:
  - Displays available funds, ledger balance, account scheme (Savings/Current), and remaining daily withdrawal limit.
- **Fast Cash**:
  - Instant one-touch withdrawals: **₹100, ₹500, ₹1,000, ₹2,000, ₹5,000**.
- **Smart Cash Withdrawal & Physical Note Dispenser**:
  - Validates balance, remaining daily withdrawal limits, and ATM Cash Vault notes.
  - Dispenses realistic currency note denominations (**₹500, ₹200, ₹100**) using an optimized dispensing algorithm.
- **Cash Deposit**:
  - Supports direct amount deposit or note-by-note inventory counting.
- **Inter-Account Funds Transfer**:
  - Recipient account verification with preview (owner name & account type) before transfer confirmation.
  - Prevents self-transfers and non-existent transfers with atomic consistency.
- **Mini-Statement & Transaction Filtering**:
  - Rich tabular transaction history (TXN ID, Timestamp, Type, Amount, Balance, Note Breakdown, Description).
  - Filter by: *Deposits Only*, *Withdrawals Only*, *Transfers Only*, or *Mini-Statement (Last 5)*.
- **Security PIN Management**:
  - PIN change with current PIN verification, 4–6 numeric digit enforcement, and confirmation.
- **Physical Receipt Generator**:
  - Styled printable ASCII transaction slips displayed on screen and saved directly as `.txt` files in `receipts/`.

---

### 🛡️ 2. Bank Administrator Portal
- Access via **Admin ID** (`admin`) and **Passcode** (`9999`).
- **View All Accounts**: Real-time overview of all customer accounts, balances, and lock states.
- **Unlock Customer Accounts**: Reset lockout status for accounts locked due to failed PIN attempts.
- **Customer Registration**: Open and seed new Savings or Current accounts on the fly.
- **ATM Cash Vault Management**: Inspect note counts and restock ATM physical cash inventory.
- **Bank System Analytics**: Total bank customer deposits, lifetime transactions count, and ATM cash ratio.

---

### 💾 3. Persistence Engine
- Built-in lightweight JSON storage in `data/atm_data.json` without any third-party library dependencies.
- Auto-seeds realistic sample accounts on initial run.
- Auto-saves all balances, transactions, lockouts, and vault inventory after every operation.

---

## 🚀 How to Run

### Quick Start (Linux / macOS / Git Bash)
```bash
./run.sh
```

### Manual Compilation & Execution
```bash
mkdir -p out data receipts
javac -d out src/*.java
java -cp out Main
```

---

## 🧪 Running Automated Tests

Run the built-in test suite (56 automated tests covering authentication, transfers, limits, denomination dispenser, persistence, and admin tools):

```bash
./test.sh
# or manually:
java -cp out TestRunner
```

---

## 🔑 Demo Credentials

### Customer Accounts
| User ID | PIN  | Account Holder       | Account Type | Starting Balance | Daily Limit |
|:-------:|:----:|:---------------------|:------------:|:----------------:|:-----------:|
| `1001`  | `1234` | Manjari PA           | SAVINGS      | ₹5,000.00        | ₹20,000.00  |
| `1002`  | `4321` | Arjun Kumar          | SAVINGS      | ₹12,500.50       | ₹25,000.00  |
| `1003`  | `1111` | Divya Sree           | SAVINGS      | ₹750.00          | ₹15,000.00  |
| `1004`  | `5555` | TechCorp Enterprise  | CURRENT      | ₹85,000.00       | ₹1,00,000.00|

### Administrator Portal
| Admin ID | Passcode |
|:--------:|:--------:|
| `admin`  | `9999`   |

---

## 📂 Project Architecture

```
atm-project/
├── data/                    # Auto-generated persistent JSON storage
│   └── atm_data.json
├── receipts/                # Auto-saved transaction receipt text files
│   └── receipt_TXN-10001.txt
├── src/
│   ├── Account.java         # Customer account model & daily limits
│   ├── ATM.java             # Terminal UX, session navigation & flows
│   ├── Bank.java            # Bank directory, transfers, admin auth & stats
│   ├── CashVault.java       # ATM physical note vault & dispenser algorithm
│   ├── ConsoleUI.java       # ANSI styling, banners, tables & receipt formatter
│   ├── Main.java            # Main entry point
│   ├── StorageManager.java  # Pure Java JSON persistence engine
│   ├── TestRunner.java      # Comprehensive automated unit test suite
│   └── Transaction.java     # Immutable transaction record model
├── run.sh                   # Build & run helper script
├── test.sh                  # Automated test runner script
└── README.md                # Project documentation
```
