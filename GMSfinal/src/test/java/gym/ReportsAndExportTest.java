package gym;

import gym.model.PlanConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReportsAndExportTest {

    @BeforeAll
    public static void setUp() {
        Database.initialize();
    }

    @Test
    public void testSettingsPersistence() throws SQLException {
        Database.setSetting("test_key", "test_val_123");
        String val = Database.getSetting("test_key", "default");
        assertEquals("test_val_123", val);

        // Test plan price get/set
        Database.setSetting("plan_monthly_price", "1800");
        double price = Database.getPlanPrice("Monthly");
        assertEquals(1800.0, price);

        // Reset to default
        Database.setSetting("plan_monthly_price", "1500");
    }

    @Test
    public void testAttendanceTrendMap() throws SQLException {
        Map<LocalDate, Integer> trend = Database.getAttendanceTrend(7);
        assertNotNull(trend);
        assertEquals(7, trend.size(), "Should contain exactly 7 days");
        assertTrue(trend.containsKey(LocalDate.now()));
    }

    @Test
    public void testRevenueTrendMap() throws SQLException {
        Map<String, Double> trend = Database.getMonthlyRevenueTrend(6);
        assertNotNull(trend);
        assertEquals(6, trend.size(), "Should contain exactly 6 months");
    }

    @Test
    public void testExpiringMembersQuery() throws SQLException {
        var expiring = Database.getExpiringMembers(365);
        assertNotNull(expiring);
        // Expirations must be ordered by expiry date
        for (int i = 0; i < expiring.size() - 1; i++) {
            assertFalse(expiring.get(i).getExpiryDate().isAfter(expiring.get(i + 1).getExpiryDate()));
        }
    }
}

