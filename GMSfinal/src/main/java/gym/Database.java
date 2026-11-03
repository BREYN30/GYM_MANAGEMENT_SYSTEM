package gym;

import gym.model.AttendanceRecord;
import gym.model.PaymentRecord;
import gym.model.RenewalRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Robust SQLite Database manager with automatic migrations, parameterized queries,
 * transactions, and modules for Members, Attendance, Payments, Renewals, and Settings.
 */
public final class Database {
    private static final Path DB_PATH = resolveDatabasePath();
    private static final String URL = "jdbc:sqlite:" + DB_PATH.toString().replace('\\', '/');
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private Database() {}

    /**
     * Dynamically resolves the database path, allowing execution from either the project
     * subfolder (GMSfinal) or the root workspace (GYMfinal2).
     */
    private static Path resolveDatabasePath() {
        Path pDirect = Paths.get("data", "gym.db");
        if (Files.exists(pDirect) && Files.isRegularFile(pDirect)) {
            return pDirect.toAbsolutePath();
        }

        Path pSub = Paths.get("GMSfinal", "data", "gym.db");
        if (Files.exists(pSub) && Files.isRegularFile(pSub)) {
            return pSub.toAbsolutePath();
        }

        Path pParent = Paths.get("..", "data", "gym.db");
        if (Files.exists(pParent) && Files.isRegularFile(pParent)) {
            return pParent.toAbsolutePath();
        }

        if (Files.isDirectory(Paths.get("GMSfinal"))) {
            return Paths.get("GMSfinal", "data", "gym.db").toAbsolutePath();
        }

        return pDirect.toAbsolutePath();
    }

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}

        Connection conn = DriverManager.getConnection(URL);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static Path getDatabasePath() {
        return DB_PATH;
    }

    public static synchronized void initialize() {
        try {
            if (DB_PATH.getParent() != null) {
                Files.createDirectories(DB_PATH.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not create database directory: " + e.getMessage(), e);
        }

        try (Connection c = connect(); Statement s = c.createStatement()) {
            // 1. Base members table
            s.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    plan TEXT NOT NULL CHECK(plan IN ('Monthly','Quarterly','Yearly')),
                    join_date TEXT NOT NULL,
                    expiry_date TEXT NOT NULL
                );
            """);

            // 2. Non-destructive migration: add missing columns if they don't exist yet
            Set<String> memberColumns = new HashSet<>();
            try (ResultSet rs = s.executeQuery("PRAGMA table_info(members);")) {
                while (rs.next()) {
                    memberColumns.add(rs.getString("name").toLowerCase(Locale.ROOT));
                }
            }

            if (!memberColumns.contains("email")) {
                s.execute("ALTER TABLE members ADD COLUMN email TEXT;");
            }
            if (!memberColumns.contains("age")) {
                s.execute("ALTER TABLE members ADD COLUMN age INTEGER;");
            }
            if (!memberColumns.contains("gender")) {
                s.execute("ALTER TABLE members ADD COLUMN gender TEXT;");
            }
            if (!memberColumns.contains("address")) {
                s.execute("ALTER TABLE members ADD COLUMN address TEXT;");
            }
            if (!memberColumns.contains("emergency_contact")) {
                s.execute("ALTER TABLE members ADD COLUMN emergency_contact TEXT;");
            }

            // 3. Attendance table
            s.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER NOT NULL,
                    check_in_date TEXT NOT NULL,
                    check_in_time TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
                );
            """);

            // 4. Payments table
            s.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    txn_ref TEXT UNIQUE NOT NULL,
                    member_id INTEGER NOT NULL,
                    plan TEXT NOT NULL,
                    amount REAL NOT NULL,
                    payment_method TEXT NOT NULL,
                    payment_date TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
                );
            """);

            // 5. Renewals history table
            s.execute("""
                CREATE TABLE IF NOT EXISTS renewals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    member_id INTEGER NOT NULL,
                    plan TEXT NOT NULL,
                    previous_expiry TEXT NOT NULL,
                    new_expiry TEXT NOT NULL,
                    renewal_date TEXT NOT NULL,
                    amount REAL NOT NULL DEFAULT 0.0,
                    FOREIGN KEY(member_id) REFERENCES members(id) ON DELETE CASCADE
                );
            """);

            // 6. Settings table
            s.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """);

            // Default settings
            s.execute("INSERT OR IGNORE INTO settings(key, value) VALUES('plan_monthly_price', '1500');");
            s.execute("INSERT OR IGNORE INTO settings(key, value) VALUES('plan_quarterly_price', '4000');");
            s.execute("INSERT OR IGNORE INTO settings(key, value) VALUES('plan_yearly_price', '12000');");
            s.execute("INSERT OR IGNORE INTO settings(key, value) VALUES('currency_symbol', '₹');");
            s.execute("INSERT OR IGNORE INTO settings(key, value) VALUES('gym_name', 'ENCORE FITNESS CLUB');");
            s.execute("INSERT OR IGNORE INTO settings(key, value) VALUES('gym_subtitle', 'PREMIUM HEALTH & WELLNESS');");

            // Seed initial founding member if members table is completely empty
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM members;")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    s.execute("""
                        INSERT OR IGNORE INTO members (id, name, phone, plan, join_date, expiry_date, email, gender)
                        VALUES (1, 'Rahul Gupta', '7977906044', 'Quarterly', '2026-10-20', '2027-01-20', 'rahul.gupta@example.com', 'Male');
                    """);
                }
            }

            // Indexes for speed
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_name ON members(name);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_members_phone ON members(phone);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(check_in_date);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_attendance_member ON attendance(member_id);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(payment_date);");
            s.execute("CREATE INDEX IF NOT EXISTS idx_payments_member ON payments(member_id);");

        } catch (SQLException e) {
            throw new IllegalStateException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    public static boolean backupDatabase(Path destination) {
        try {
            if (!Files.exists(DB_PATH)) {
                return false;
            }
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            Files.copy(DB_PATH, destination, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ==========================================
    // STATS
    // ==========================================

    public record GymStats(
        int totalMembers,
        int activeMembers,
        int expiringSoonMembers,
        int expiredMembers,
        int todayAttendance,
        double monthlyRevenue,
        double totalRevenue,
        int monthlyCount,
        int quarterlyCount,
        int yearlyCount
    ) {}

    public static GymStats calculateStats() throws SQLException {
        List<Member> members = getAllMembers();
        int active = 0, soon = 0, expired = 0, mo = 0, qu = 0, ye = 0;
        for (Member m : members) {
            String status = m.getStatus();
            if ("Active".equalsIgnoreCase(status)) {
                active++;
            } else if ("Expiring Soon".equalsIgnoreCase(status)) {
                soon++;
            } else {
                expired++;
            }

            switch (m.getPlan() != null ? m.getPlan() : "") {
                case "Quarterly" -> qu++;
                case "Yearly" -> ye++;
                default -> mo++;
            }
        }

        int todayAttendance = getTodayAttendanceCount();
        double monthlyRev = getCurrentMonthRevenue();
        double totalRev = getTotalRevenue();

        return new GymStats(
            members.size(), active, soon, expired,
            todayAttendance, monthlyRev, totalRev,
            mo, qu, ye
        );
    }

    // ==========================================
    // MEMBERS CRUD & SEARCH
    // ==========================================

    public static List<Member> getAllMembers() throws SQLException {
        List<Member> out = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY id DESC";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                out.add(readMember(r));
            }
        }
        return out;
    }

    public static Member getMemberById(int id) throws SQLException {
        String sql = "SELECT * FROM members WHERE id = ?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return readMember(r);
            }
        }
        return null;
    }

    public static List<Member> searchAndFilterMembers(String q, String status, String sort) throws SQLException {
        return searchAndFilterMembers(q, status, "All Plans", sort);
    }

    public static List<Member> searchAndFilterMembers(String q, String status, String planFilter, String sort) throws SQLException {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Member> all = getAllMembers();
        List<Member> filtered = new ArrayList<>();

        for (Member m : all) {
            boolean textMatch = query.isEmpty()
                || m.getName().toLowerCase(Locale.ROOT).contains(query)
                || m.getPhone().contains(query)
                || String.valueOf(m.getId()).equals(query)
                || m.getFormattedId().toLowerCase(Locale.ROOT).contains(query)
                || (m.getEmail() != null && m.getEmail().toLowerCase(Locale.ROOT).contains(query));

            boolean statusMatch = status == null || status.equals("All Statuses") || status.equals("All") || m.getStatus().equalsIgnoreCase(status);
            boolean planMatch = planFilter == null || planFilter.equals("All Plans") || planFilter.equals("All") || m.getPlan().equalsIgnoreCase(planFilter);

            if (textMatch && statusMatch && planMatch) {
                filtered.add(m);
            }
        }

        Comparator<Member> comp = switch (sort == null ? "" : sort) {
            case "ID (Oldest First)" -> Comparator.comparingInt(Member::getId);
            case "Name (A-Z)" -> Comparator.comparing(Member::getName, String.CASE_INSENSITIVE_ORDER);
            case "Name (Z-A)" -> Comparator.comparing(Member::getName, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Expiry Date" -> Comparator.comparing(Member::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Join Date" -> Comparator.comparing(Member::getJoinDate, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparingInt(Member::getId).reversed();
        };

        filtered.sort(comp);
        return filtered;
    }

    public static int addMember(String n, String ph, String plan, String join, String expiry) throws SQLException {
        return addMember(n, ph, null, null, null, plan, join, expiry, null, null);
    }

    public static int addMember(String name, String phone, String email, Integer age, String gender,
                                String plan, String joinDate, String expiryDate, String address, String emergencyContact) throws SQLException {
        if (joinDate == null || joinDate.trim().isEmpty()) {
            joinDate = LocalDate.now().toString();
        }
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            LocalDate jd = parseDateSafe(joinDate);
            expiryDate = Member.calculateExpiryDate(jd, plan).toString();
        }

        String sql = """
            INSERT INTO members (name, phone, email, age, gender, plan, join_date, expiry_date, address, emergency_contact)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name.trim());
            p.setString(2, phone.trim());
            p.setString(3, (email != null && !email.trim().isEmpty()) ? email.trim() : null);
            if (age != null && age > 0) p.setInt(4, age); else p.setNull(4, Types.INTEGER);
            p.setString(5, (gender != null && !gender.trim().isEmpty()) ? gender.trim() : null);
            p.setString(6, plan);
            p.setString(7, joinDate.trim());
            p.setString(8, expiryDate.trim());
            p.setString(9, (address != null && !address.trim().isEmpty()) ? address.trim() : null);
            p.setString(10, (emergencyContact != null && !emergencyContact.trim().isEmpty()) ? emergencyContact.trim() : null);

            p.executeUpdate();
            try (ResultSet gk = p.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public static void updateMember(int id, String n, String ph, String plan, String join, String expiry) throws SQLException {
        updateMember(id, n, ph, null, null, null, plan, join, expiry, null, null);
    }

    public static void updateMember(int id, String name, String phone, String email, Integer age, String gender,
                                   String plan, String joinDate, String expiryDate, String address, String emergencyContact) throws SQLException {
        if (joinDate == null || joinDate.trim().isEmpty()) {
            joinDate = LocalDate.now().toString();
        }
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            LocalDate jd = parseDateSafe(joinDate);
            expiryDate = Member.calculateExpiryDate(jd, plan).toString();
        }

        String sql = """
            UPDATE members
            SET name = ?, phone = ?, email = ?, age = ?, gender = ?, plan = ?, join_date = ?, expiry_date = ?,
                address = ?, emergency_contact = ?
            WHERE id = ?
        """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name.trim());
            p.setString(2, phone.trim());
            p.setString(3, (email != null && !email.trim().isEmpty()) ? email.trim() : null);
            if (age != null && age > 0) p.setInt(4, age); else p.setNull(4, Types.INTEGER);
            p.setString(5, (gender != null && !gender.trim().isEmpty()) ? gender.trim() : null);
            p.setString(6, plan);
            p.setString(7, joinDate.trim());
            p.setString(8, expiryDate.trim());
            p.setString(9, (address != null && !address.trim().isEmpty()) ? address.trim() : null);
            p.setString(10, (emergencyContact != null && !emergencyContact.trim().isEmpty()) ? emergencyContact.trim() : null);
            p.setInt(11, id);

            if (p.executeUpdate() == 0) {
                throw new SQLException("Member #" + id + " not found");
            }
        }
    }

    public static void renewMember(int id, String plan, String newExpiry) throws SQLException {
        renewMemberWithRecord(id, plan, newExpiry, 0.0, null);
    }

    public static void renewMemberWithRecord(int memberId, String plan, String newExpiryDate, double amount, String paymentMethod) throws SQLException {
        Member member = getMemberById(memberId);
        if (member == null) throw new SQLException("Member #" + memberId + " not found");

        LocalDate prevExpiry = member.getExpiryDate();
        LocalDate newExpiry = parseDateSafe(newExpiryDate);
        LocalDate today = LocalDate.now();

        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                // 1. Update member
                try (PreparedStatement p = c.prepareStatement("UPDATE members SET plan = ?, expiry_date = ? WHERE id = ?")) {
                    p.setString(1, plan);
                    p.setString(2, newExpiry.toString());
                    p.setInt(3, memberId);
                    p.executeUpdate();
                }

                // 2. Record renewal history
                try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO renewals (member_id, plan, previous_expiry, new_expiry, renewal_date, amount) VALUES (?, ?, ?, ?, ?, ?)"
                )) {
                    p.setInt(1, memberId);
                    p.setString(2, plan);
                    p.setString(3, prevExpiry != null ? prevExpiry.toString() : today.toString());
                    p.setString(4, newExpiry.toString());
                    p.setString(5, today.toString());
                    p.setDouble(6, amount);
                    p.executeUpdate();
                }

                // 3. Record payment if amount > 0
                if (amount > 0 && paymentMethod != null && !paymentMethod.trim().isEmpty()) {
                    String txnRef = generateTxnRef();
                    try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO payments (txn_ref, member_id, plan, amount, payment_method, payment_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    )) {
                        p.setString(1, txnRef);
                        p.setInt(2, memberId);
                        p.setString(3, plan);
                        p.setDouble(4, amount);
                        p.setString(5, paymentMethod);
                        p.setString(6, today.toString());
                        p.setString(7, "Membership Renewal (" + plan + " until " + newExpiry + ")");
                        p.executeUpdate();
                    }
                }

                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public static void deleteMember(int id) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("DELETE FROM attendance WHERE member_id = ?")) {
                    p.setInt(1, id);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM payments WHERE member_id = ?")) {
                    p.setInt(1, id);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM renewals WHERE member_id = ?")) {
                    p.setInt(1, id);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM members WHERE id = ?")) {
                    p.setInt(1, id);
                    if (p.executeUpdate() == 0) throw new SQLException("Member not found");
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public static List<Member> getExpiringMembers(int daysAhead) throws SQLException {
        List<Member> all = getAllMembers();
        List<Member> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(daysAhead);

        for (Member m : all) {
            LocalDate exp = m.getExpiryDate();
            if (exp != null && !exp.isBefore(today) && !exp.isAfter(maxDate)) {
                result.add(m);
            }
        }
        result.sort(Comparator.comparing(Member::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    // ==========================================
    // ATTENDANCE MANAGEMENT
    // ==========================================

    public static boolean isAlreadyCheckedInToday(int memberId) throws SQLException {
        String today = LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM attendance WHERE member_id = ? AND check_in_date = ?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, memberId);
            p.setString(2, today);
            try (ResultSet r = p.executeQuery()) {
                return r.next() && r.getInt(1) > 0;
            }
        }
    }

    public static int recordAttendance(int memberId, String notes) throws SQLException {
        if (isAlreadyCheckedInToday(memberId)) {
            throw new SQLException("Member is already checked in for today.");
        }
        String sql = "INSERT INTO attendance (member_id, check_in_date, check_in_time, notes) VALUES (?, ?, ?, ?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, memberId);
            p.setString(2, LocalDate.now().toString());
            p.setString(3, LocalTime.now().format(TIME_FMT));
            p.setString(4, notes != null && !notes.trim().isEmpty() ? notes.trim() : "Regular Workout");
            p.executeUpdate();
            try (ResultSet gk = p.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public static int getTodayAttendanceCount() throws SQLException {
        String today = LocalDate.now().toString();
        String sql = "SELECT COUNT(*) FROM attendance WHERE check_in_date = ?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, today);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    public static List<AttendanceRecord> getAttendanceForDate(LocalDate date) throws SQLException {
        return getAttendanceFiltered("", date, date);
    }

    public static List<AttendanceRecord> getAttendanceFiltered(String query, LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.member_id, m.name, m.phone, a.check_in_date, a.check_in_time, a.notes
            FROM attendance a
            LEFT JOIN members m ON a.member_id = m.id
            WHERE 1=1
        """);
        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            sql.append(" AND a.check_in_date >= ?");
            params.add(startDate.toString());
        }
        if (endDate != null) {
            sql.append(" AND a.check_in_date <= ?");
            params.add(endDate.toString());
        }
        if (query != null && !query.trim().isEmpty()) {
            String cleanQuery = query.trim().toLowerCase(Locale.ROOT);
            String numericId = cleanQuery.replaceAll("[^0-9]", "");
            sql.append(" AND (LOWER(COALESCE(m.name, '')) LIKE ? OR COALESCE(m.phone, '') LIKE ? OR CAST(a.member_id AS TEXT) = ? OR LOWER(COALESCE(a.notes, '')) LIKE ?)");
            String q = "%" + cleanQuery + "%";
            params.add(q);
            params.add(q);
            params.add(!numericId.isEmpty() ? numericId : cleanQuery);
            params.add(q);
        }

        sql.append(" ORDER BY a.check_in_date DESC, a.id DESC");

        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new AttendanceRecord(
                        r.getInt("id"),
                        r.getInt("member_id"),
                        r.getString("name") != null ? r.getString("name") : "Deleted Member",
                        r.getString("phone") != null ? r.getString("phone") : "—",
                        parseDateSafe(r.getString("check_in_date")),
                        r.getString("check_in_time"),
                        r.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    public static List<AttendanceRecord> getAttendanceHistory(int memberId) throws SQLException {
        String sql = """
            SELECT a.id, a.member_id, m.name, m.phone, a.check_in_date, a.check_in_time, a.notes
            FROM attendance a
            JOIN members m ON a.member_id = m.id
            WHERE a.member_id = ?
            ORDER BY a.check_in_date DESC, a.id DESC
        """;
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, memberId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new AttendanceRecord(
                        r.getInt("id"),
                        r.getInt("member_id"),
                        r.getString("name"),
                        r.getString("phone"),
                        parseDateSafe(r.getString("check_in_date")),
                        r.getString("check_in_time"),
                        r.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    public static void deleteAttendance(int id) throws SQLException {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("DELETE FROM attendance WHERE id = ?")) {
            p.setInt(1, id);
            p.executeUpdate();
        }
    }

    public static Map<LocalDate, Integer> getAttendanceTrend(int pastDays) throws SQLException {
        Map<LocalDate, Integer> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = pastDays - 1; i >= 0; i--) {
            trend.put(today.minusDays(i), 0);
        }

        LocalDate startDate = today.minusDays(pastDays - 1);
        String sql = "SELECT check_in_date, COUNT(*) AS cnt FROM attendance WHERE check_in_date >= ? AND check_in_date <= ? GROUP BY check_in_date";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, startDate.toString());
            p.setString(2, today.toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    try {
                        LocalDate d = parseDateSafe(r.getString("check_in_date"));
                        if (trend.containsKey(d)) {
                            trend.put(d, r.getInt("cnt"));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return trend;
    }

    // ==========================================
    // PAYMENTS MANAGEMENT
    // ==========================================

    public static String generateTxnRef() {
        String datePart = LocalDate.now().toString().replace("-", "");
        long timePart = System.currentTimeMillis() % 100000;
        int randomPart = (int) (100 + Math.random() * 900);
        return "TXN-" + datePart + "-" + String.format("%05d%03d", timePart, randomPart);
    }

    public static String recordPayment(int memberId, String plan, double amount, String paymentMethod, String notes) throws SQLException {
        String txnRef = generateTxnRef();
        String sql = """
            INSERT INTO payments (txn_ref, member_id, plan, amount, payment_method, payment_date, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, txnRef);
            p.setInt(2, memberId);
            p.setString(3, plan);
            p.setDouble(4, amount);
            p.setString(5, paymentMethod);
            p.setString(6, LocalDate.now().toString());
            p.setString(7, notes != null ? notes.trim() : "Membership fee");
            p.executeUpdate();
        }
        return txnRef;
    }

    public static List<PaymentRecord> getPaymentsFiltered(String query, LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT p.id, p.txn_ref, p.member_id, m.name, p.plan, p.amount, p.payment_method, p.payment_date, p.notes
            FROM payments p
            LEFT JOIN members m ON p.member_id = m.id
            WHERE 1=1
        """);
        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            sql.append(" AND p.payment_date >= ?");
            params.add(startDate.toString());
        }
        if (endDate != null) {
            sql.append(" AND p.payment_date <= ?");
            params.add(endDate.toString());
        }
        if (query != null && !query.trim().isEmpty()) {
            String cleanQuery = query.trim().toLowerCase(Locale.ROOT);
            String numericId = cleanQuery.replaceAll("[^0-9]", "");
            sql.append(" AND (LOWER(p.txn_ref) LIKE ? OR LOWER(COALESCE(m.name, '')) LIKE ? OR CAST(p.member_id AS TEXT) = ? OR LOWER(COALESCE(p.notes, '')) LIKE ?)");
            String q = "%" + cleanQuery + "%";
            params.add(q);
            params.add(q);
            params.add(!numericId.isEmpty() ? numericId : cleanQuery);
            params.add(q);
        }

        sql.append(" ORDER BY p.payment_date DESC, p.id DESC");

        List<PaymentRecord> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                p.setObject(i + 1, params.get(i));
            }
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new PaymentRecord(
                        r.getInt("id"),
                        r.getString("txn_ref"),
                        r.getInt("member_id"),
                        r.getString("name") != null ? r.getString("name") : "Deleted Member",
                        r.getString("plan"),
                        r.getDouble("amount"),
                        r.getString("payment_method"),
                        parseDateSafe(r.getString("payment_date")),
                        r.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    public static List<PaymentRecord> getPaymentsForMember(int memberId) throws SQLException {
        String sql = """
            SELECT p.id, p.txn_ref, p.member_id, m.name, p.plan, p.amount, p.payment_method, p.payment_date, p.notes
            FROM payments p
            JOIN members m ON p.member_id = m.id
            WHERE p.member_id = ?
            ORDER BY p.payment_date DESC, p.id DESC
        """;
        List<PaymentRecord> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, memberId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    list.add(new PaymentRecord(
                        r.getInt("id"),
                        r.getString("txn_ref"),
                        r.getInt("member_id"),
                        r.getString("name"),
                        r.getString("plan"),
                        r.getDouble("amount"),
                        r.getString("payment_method"),
                        parseDateSafe(r.getString("payment_date")),
                        r.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    public static double getTotalRevenue() throws SQLException {
        String sql = "SELECT SUM(amount) FROM payments";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            return (r.next() && r.getObject(1) != null) ? r.getDouble(1) : 0.0;
        }
    }

    public static double getCurrentMonthRevenue() throws SQLException {
        LocalDate now = LocalDate.now();
        String currentYm = String.format("%d-%02d", now.getYear(), now.getMonthValue());
        String sql = "SELECT SUM(amount) FROM payments WHERE substr(payment_date, 1, 7) = ?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, currentYm);
            try (ResultSet r = p.executeQuery()) {
                return (r.next() && r.getObject(1) != null) ? r.getDouble(1) : 0.0;
            }
        }
    }

    public static Map<String, Double> getMonthlyRevenueTrend(int months) throws SQLException {
        Map<String, Double> trend = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate m = now.withDayOfMonth(1).minusMonths(i);
            String key = String.format("%d-%02d", m.getYear(), m.getMonthValue());
            trend.put(key, 0.0);
        }

        LocalDate start = now.withDayOfMonth(1).minusMonths(months - 1);
        String sql = "SELECT substr(payment_date, 1, 7) as ym, SUM(amount) as total FROM payments WHERE payment_date >= ? GROUP BY ym";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, start.toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String ym = r.getString("ym");
                    if (trend.containsKey(ym)) {
                        trend.put(ym, r.getDouble("total"));
                    }
                }
            }
        }
        return trend;
    }

    // ==========================================
    // RENEWALS MANAGEMENT
    // ==========================================

    public static List<RenewalRecord> getAllRenewals() throws SQLException {
        String sql = """
            SELECT r.id, r.member_id, m.name, r.plan, r.previous_expiry, r.new_expiry, r.renewal_date, r.amount
            FROM renewals r
            LEFT JOIN members m ON r.member_id = m.id
            ORDER BY r.renewal_date DESC, r.id DESC
        """;
        List<RenewalRecord> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql); ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                list.add(new RenewalRecord(
                    rs.getInt("id"),
                    rs.getInt("member_id"),
                    rs.getString("name") != null ? rs.getString("name") : "Deleted Member",
                    rs.getString("plan"),
                    parseDateSafe(rs.getString("previous_expiry")),
                    parseDateSafe(rs.getString("new_expiry")),
                    parseDateSafe(rs.getString("renewal_date")),
                    rs.getDouble("amount")
                ));
            }
        }
        return list;
    }

    public static List<RenewalRecord> getRenewalsForMember(int memberId) throws SQLException {
        String sql = """
            SELECT r.id, r.member_id, m.name, r.plan, r.previous_expiry, r.new_expiry, r.renewal_date, r.amount
            FROM renewals r
            JOIN members m ON r.member_id = m.id
            WHERE r.member_id = ?
            ORDER BY r.renewal_date DESC, r.id DESC
        """;
        List<RenewalRecord> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, memberId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    list.add(new RenewalRecord(
                        rs.getInt("id"),
                        rs.getInt("member_id"),
                        rs.getString("name"),
                        rs.getString("plan"),
                        parseDateSafe(rs.getString("previous_expiry")),
                        parseDateSafe(rs.getString("new_expiry")),
                        parseDateSafe(rs.getString("renewal_date")),
                        rs.getDouble("amount")
                    ));
                }
            }
        }
        return list;
    }

    // ==========================================
    // SETTINGS MANAGEMENT
    // ==========================================

    public static String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, key);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getString("value");
            }
        } catch (SQLException ignored) {}
        return defaultValue;
    }

    public static void setSetting(String key, String value) throws SQLException {
        String sql = "INSERT INTO settings(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, key);
            p.setString(2, value);
            p.executeUpdate();
        }
    }

    public static double getPlanPrice(String plan) {
        String p = plan == null ? "Monthly" : plan.trim();
        String key = switch (p) {
            case "Quarterly" -> "plan_quarterly_price";
            case "Yearly" -> "plan_yearly_price";
            default -> "plan_monthly_price";
        };
        String def = switch (p) {
            case "Quarterly" -> "4000";
            case "Yearly" -> "12000";
            default -> "1500";
        };
        try {
            return Double.parseDouble(getSetting(key, def));
        } catch (NumberFormatException e) {
            return 1500.0;
        }
    }

    // ==========================================
    // HELPER: Date Parser & ResultSet Reader
    // ==========================================

    public static LocalDate parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }
        String clean = dateStr.trim();
        if (clean.length() > 10) {
            clean = clean.substring(0, 10);
        }
        try {
            return LocalDate.parse(clean);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private static Member readMember(ResultSet r) throws SQLException {
        try {
            int id = r.getInt("id");
            String name = r.getString("name");
            String phone = r.getString("phone");
            String plan = r.getString("plan");
            LocalDate joinDate = parseDateSafe(r.getString("join_date"));
            LocalDate expiryDate = parseDateSafe(r.getString("expiry_date"));

            String email = null;
            Integer age = null;
            String gender = null;
            String address = null;
            String emergencyContact = null;

            try { email = r.getString("email"); } catch (SQLException ignored) {}
            try {
                int a = r.getInt("age");
                if (!r.wasNull()) age = a;
            } catch (SQLException ignored) {}
            try { gender = r.getString("gender"); } catch (SQLException ignored) {}
            try { address = r.getString("address"); } catch (SQLException ignored) {}
            try { emergencyContact = r.getString("emergency_contact"); } catch (SQLException ignored) {}

            return new Member(id, name, phone, email, age, gender, plan, joinDate, expiryDate, address, emergencyContact);
        } catch (RuntimeException e) {
            throw new SQLException("Failed to parse member record: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // MAIN METHOD (Quick Diagnostic & Direct Launch from VS Code)
    // ==========================================

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  ENCORE Pro — Gym Management System Database     ");
        System.out.println("==================================================");
        try {
            initialize();
            System.out.println("[✓] Database initialized at: " + getDatabasePath());

            GymStats stats = calculateStats();
            System.out.println("[✓] Verified Members in DB: " + stats.totalMembers());
            System.out.println("[✓] Active: " + stats.activeMembers() + " | Expiring Soon: " + stats.expiringSoonMembers() + " | Expired: " + stats.expiredMembers());
            System.out.println("[✓] Today's Attendance Visits: " + stats.todayAttendance());
            System.out.println("[✓] Launching GUI application (GymApp)...");
            System.out.println("==================================================");

            GymApp.main(args);
        } catch (Exception e) {
            System.err.println("[X] Database startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
