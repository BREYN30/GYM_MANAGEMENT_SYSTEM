package gym;

import gym.model.AttendanceRecord;
import gym.model.PaymentRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    @BeforeAll
    public static void setUp() {
        Database.initialize();
    }

    @Test
    public void testDatabaseInitializationPreservesRahulGupta() throws SQLException {
        List<Member> members = Database.getAllMembers();
        assertFalse(members.isEmpty(), "Members table should have existing data");
        boolean foundRahul = members.stream().anyMatch(m -> m.getName().equalsIgnoreCase("Rahul Gupta"));
        assertTrue(foundRahul, "Rahul Gupta record must be preserved");
    }

    @Test
    public void testAddAndSearchMember() throws SQLException {
        String testPhone = "9988776655";
        // Clean up any old test record with this phone if present
        List<Member> existing = Database.searchAndFilterMembers(testPhone, "All Statuses", "ID (Newest First)");
        for (Member m : existing) {
            Database.deleteMember(m.getId());
        }

        LocalDate today = LocalDate.now();
        int newId = Database.addMember(
            "Test Member Alpha",
            testPhone,
            "alpha@gym.com",
            26,
            "Male",
            "Monthly",
            today.toString(),
            today.plusMonths(1).toString(),
            "123 Fitness St",
            "Emergency: 9999999999"
        );
        assertTrue(newId > 0, "Generated ID should be positive");

        Member retrieved = Database.getMemberById(newId);
        assertNotNull(retrieved);
        assertEquals("Test Member Alpha", retrieved.getName());
        assertEquals("alpha@gym.com", retrieved.getEmail());
        assertEquals(26, retrieved.getAge());
        assertEquals("Monthly", retrieved.getPlan());

        // Clean up test member
        Database.deleteMember(newId);
        assertNull(Database.getMemberById(newId));
    }

    @Test
    public void testAttendanceCheckInAndDuplicatePrevention() throws SQLException {
        // Create temporary member for attendance
        LocalDate today = LocalDate.now();
        int memberId = Database.addMember(
            "Attendance Tester",
            "8877665544",
            null,
            null,
            "Female",
            "Monthly",
            today.toString(),
            today.plusMonths(1).toString(),
            null,
            null
        );

        try {
            assertFalse(Database.isAlreadyCheckedInToday(memberId));
            int attId = Database.recordAttendance(memberId, "Morning Cardio");
            assertTrue(attId > 0);
            assertTrue(Database.isAlreadyCheckedInToday(memberId));

            // Attempt duplicate check-in
            assertThrows(SQLException.class, () -> Database.recordAttendance(memberId, "Duplicate"));

            List<AttendanceRecord> records = Database.getAttendanceForDate(today);
            boolean found = records.stream().anyMatch(a -> a.memberId() == memberId);
            assertTrue(found, "Attendance entry must be present in today's attendance list");
        } finally {
            Database.deleteMember(memberId);
        }
    }

    @Test
    public void testPaymentRecording() throws SQLException {
        LocalDate today = LocalDate.now();
        int memberId = Database.addMember(
            "Payment Tester",
            "7766554433",
            "pay@test.com",
            null,
            null,
            "Quarterly",
            today.toString(),
            today.plusMonths(3).toString(),
            null,
            null
        );

        try {
            String txnRef = Database.recordPayment(memberId, "Quarterly", 4000.0, "UPI", "Quarterly Fee");
            assertNotNull(txnRef);
            assertTrue(txnRef.startsWith("TXN-"));

            List<PaymentRecord> payments = Database.getPaymentsForMember(memberId);
            assertEquals(1, payments.size());
            PaymentRecord p = payments.get(0);
            assertEquals(4000.0, p.amount());
            assertEquals("UPI", p.paymentMethod());
            assertEquals("Quarterly", p.plan());
        } finally {
            Database.deleteMember(memberId);
        }
    }

    @Test
    public void testGymStatsCalculation() throws SQLException {
        Database.GymStats stats = Database.calculateStats();
        assertNotNull(stats);
        assertTrue(stats.totalMembers() >= 1, "Must have at least Rahul Gupta");
        assertTrue(stats.activeMembers() + stats.expiringSoonMembers() + stats.expiredMembers() == stats.totalMembers());
    }
}

