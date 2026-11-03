# ENCORE Pro — Gym Management System

**ENCORE Pro** is an advanced, production-grade desktop Gym Management System built with **Java (JDK 17+)**, **Java Swing**, and **SQLite (JDBC)**. Featuring a modern **Midnight Violet Gradient** interface, it provides complete end-to-end gym administration including member lifecycle management, attendance tracking, billing & receipts, financial reporting, and plan configurations.

---

## 🚀 Key Features

1. **Brand Identity & Midnight Violet Theme**
   - High-contrast dark elevation theme: `#0B1020` primary background, `#11182B` sidebar, `#171F35` cards.
   - Tasteful electric violet (`#7C3AED` to `#9333EA`) and cyan (`#22D3EE`) accents.
   - Smooth custom-painted 2D components: gradient action buttons, rounded elevation cards, status pills, and dark table row striping.

2. **Advanced Real-Time Dashboard**
   - Telemetry metric cards: Total Members, Active Members, Expiring Soon, Expired, Today's Attendance, and Monthly Revenue.
   - **Custom Java 2D vector charts**:
     - **Membership Plan Donut Chart** (Monthly, Quarterly, Yearly distribution with center metric).
     - **7-Day Attendance Trend Bar Chart** (anti-aliased bars, grid lines, and labels).
     - **Monthly Revenue Trend Bar Chart** (compact currency formatting).
   - Live alert tables for expiring memberships (within 7 days) and recent member registrations.
   - Header with brand mark, live formatted date, SQLite connection status, and 1-click Quick Register.

3. **Complete Member Directory (CRUD)**
   - Capture: Auto-generated ID (`ENC-0001`), Full Name, Phone (10 digits), Email, Age, Gender, Plan, Join Date, Calculated Expiry Date, Address, and Emergency Contact.
   - Immediate payment option upon member registration.
   - Multi-field search (Name, Phone, Email, Formatted ID).
   - Filter by Status (Active, Expiring Soon, Expired) and Plan (Monthly, Quarterly, Yearly).
   - Multi-column sorting.
   - Member Details Modal: Personal info, current status, attendance visit history, and payment ledger.
   - Safe member deletion with foreign key cascading and confirmation modal.
   - One-click **CSV Export**.

4. **Memberships & Renewal Engine**
   - Monthly, Quarterly, and Yearly plan tiers.
   - Renewal calculation rules:
     - Active memberships extend from their current expiry date.
     - Expired memberships extend from today's date (`LocalDate.now()`).
     - Preserves original registration / join date.
   - Live interactive renewal date preview and amount calculation before confirmation.
   - Audited renewal history log stored in SQLite.

5. **Attendance Management & Duplicate Prevention**
   - Fast member check-in with search and workout notes (Regular, Cardio, Strength, HIIT, Personal Training).
   - Strict **same-day duplicate check-in prevention** with clear user alerts.
   - Today's visit counter and date range filters (Today, Yesterday, Last 7 Days, All Time).
   - Persistent visit log with CSV export capability.

6. **Billing, Payments & Automated Receipts**
   - Record offline payments (Cash, UPI, Card, Net Banking).
   - Unique transaction reference generator (`TXN-YYYYMMDD-XXXX`).
   - Auto-generated **Official Payment Receipt Modal** (viewable and auditable).
   - Revenue analytics: Total Revenue, Current Month Revenue, and Transaction Volume.
   - Payment export to CSV.

7. **Reports & Business Intelligence**
   - Membership Roster & Status Report.
   - Upcoming Expirations Audit (Next 30 Days forecast).
   - Financial Revenue & Ledger Report.
   - Attendance Logs & Activity Report.
   - Renewal History Report.
   - Instant CSV export for all reports.

8. **Settings & System Maintenance**
   - Configurable plan pricing (saved directly to SQLite `settings` table).
   - Configurable Gym Brand Name, Tagline, and Currency Symbol.
   - Database Tools: One-click timestamped SQLite backup (`.bak`), file size monitor, and `PRAGMA integrity_check` verification.

---

## 🛠️ Prerequisites

- **Java Development Kit (JDK 17 or higher)**
- **Apache Maven 3.8+**

---

## 💻 Build and Run Instructions

### 1. Compile and Run Unit Tests
```powershell
mvn clean test
```

### 2. Launch the Application
```powershell
mvn exec:java
```

Or compile and run directly with Maven:
```powershell
mvn compile exec:java
```

---

## 🗄️ Database Architecture

The application persists all data locally in `data/gym.db` using **SQLite + JDBC**.

### Non-Destructive Auto-Migration
Existing databases are updated automatically upon startup using `PRAGMA table_info` and non-destructive `ALTER TABLE` queries. Existing member records are completely preserved.

### Schema:
- `members` (`id`, `name`, `phone`, `email`, `age`, `gender`, `plan`, `join_date`, `expiry_date`, `address`, `emergency_contact`)
- `attendance` (`id`, `member_id`, `check_in_date`, `check_in_time`, `notes`)
- `payments` (`id`, `txn_ref`, `member_id`, `plan`, `amount`, `payment_method`, `payment_date`, `notes`)
- `renewals` (`id`, `member_id`, `plan`, `previous_expiry`, `new_expiry`, `renewal_date`, `amount`)
- `settings` (`key`, `value`)

Indexes are created on search keys (`phone`, `name`, `check_in_date`, `payment_date`) for responsive desktop performance.
