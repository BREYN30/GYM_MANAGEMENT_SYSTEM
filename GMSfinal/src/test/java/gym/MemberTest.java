package gym;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MemberTest {

    @Test
    public void testCalculateExpiryDateMonthly() {
        LocalDate start = LocalDate.of(2026, 1, 15);
        LocalDate expiry = Member.calculateExpiryDate(start, "Monthly");
        assertEquals(LocalDate.of(2026, 2, 15), expiry);
    }

    @Test
    public void testCalculateExpiryDateQuarterly() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        LocalDate expiry = Member.calculateExpiryDate(start, "Quarterly");
        // Jan 31 + 3 months -> Apr 30
        assertEquals(LocalDate.of(2026, 4, 30), expiry);
    }

    @Test
    public void testCalculateExpiryDateYearlyLeapYear() {
        LocalDate leapStart = LocalDate.of(2024, 2, 29);
        LocalDate expiry = Member.calculateExpiryDate(leapStart, "Yearly");
        // 2024 leap year + 1 year -> 2025 Feb 28
        assertEquals(LocalDate.of(2025, 2, 28), expiry);
    }

    @Test
    public void testCalculateRenewalDateActiveMembership() {
        // Current expiry is in the future
        LocalDate futureExpiry = LocalDate.now().plusDays(20);
        LocalDate newExpiry = Member.calculateRenewalDate(futureExpiry, "Monthly");
        assertEquals(futureExpiry.plusMonths(1), newExpiry);
    }

    @Test
    public void testCalculateRenewalDateExpiredMembership() {
        // Expired in past
        LocalDate pastExpiry = LocalDate.now().minusDays(10);
        LocalDate newExpiry = Member.calculateRenewalDate(pastExpiry, "Monthly");
        assertEquals(LocalDate.now().plusMonths(1), newExpiry);
    }

    @Test
    public void testMemberValidation() {
        List<String> validErrors = Member.validate("Alice Smith", "9876543210", "alice@example.com", LocalDate.now(), "Monthly");
        assertTrue(validErrors.isEmpty(), "Valid member details should have zero errors");

        List<String> invalidErrors = Member.validate("", "abc", "invalid-email", null, "InvalidPlan");
        assertFalse(invalidErrors.isEmpty(), "Invalid input must yield validation errors");
        assertTrue(invalidErrors.stream().anyMatch(e -> e.contains("name")));
        assertTrue(invalidErrors.stream().anyMatch(e -> e.contains("phone")));
        assertTrue(invalidErrors.stream().anyMatch(e -> e.contains("email")));
    }

    @Test
    public void testMemberStatusCalculations() {
        LocalDate today = LocalDate.now();
        Member active = new Member(1, "John", "1234567890", "Monthly", today, today.plusDays(30));
        assertEquals("Active", active.getStatus());

        Member expiring = new Member(2, "Bob", "1234567890", "Monthly", today, today.plusDays(3));
        assertEquals("Expiring Soon", expiring.getStatus());

        Member expired = new Member(3, "Eve", "1234567890", "Monthly", today.minusMonths(2), today.minusDays(1));
        assertEquals("Expired", expired.getStatus());
    }
}

