package gym;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a Gym Member with profile details, membership status, and validation logic.
 */
public class Member {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");

    private final int id;
    private final String name;
    private final String phone;
    private final String email;
    private final Integer age;
    private final String gender;
    private final String plan;
    private final LocalDate joinDate;
    private final LocalDate expiryDate;
    private final String address;
    private final String emergencyContact;

    /**
     * Backward-compatible 6-argument constructor.
     */
    public Member(int id, String name, String phone, String plan, LocalDate joinDate, LocalDate expiryDate) {
        this(id, name, phone, null, null, null, plan, joinDate, expiryDate, null, null);
    }

    /**
     * Full constructor capturing complete member profile.
     */
    public Member(int id, String name, String phone, String email, Integer age, String gender,
                  String plan, LocalDate joinDate, LocalDate expiryDate, String address, String emergencyContact) {
        this.id = id;
        this.name = name != null ? name.trim() : "";
        this.phone = phone != null ? phone.trim() : "";
        this.email = email != null && !email.trim().isEmpty() ? email.trim() : null;
        this.age = (age != null && age > 0) ? age : null;
        this.gender = gender != null && !gender.trim().isEmpty() ? gender.trim() : "Unspecified";
        this.plan = plan != null ? plan.trim() : "Monthly";
        this.joinDate = joinDate != null ? joinDate : LocalDate.now();
        this.expiryDate = expiryDate != null ? expiryDate : calculateExpiryDate(this.joinDate, this.plan);
        this.address = address != null && !address.trim().isEmpty() ? address.trim() : null;
        this.emergencyContact = emergencyContact != null && !emergencyContact.trim().isEmpty() ? emergencyContact.trim() : null;
    }

    public int getId() { return id; }
    public String getFormattedId() { return String.format("ENC-%04d", id); }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email != null ? email : ""; }
    public Integer getAge() { return age; }
    public String getAgeDisplay() { return age != null ? String.valueOf(age) : "—"; }
    public String getGender() { return gender != null ? gender : "Unspecified"; }
    public String getPlan() { return plan; }
    public LocalDate getJoinDate() { return joinDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getAddress() { return address != null ? address : ""; }
    public String getEmergencyContact() { return emergencyContact != null ? emergencyContact : ""; }

    public long getDaysRemaining() {
        if (expiryDate == null) return -1;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public String getStatus() {
        if (expiryDate == null) return "Unknown";
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) return "Expired";
        long days = ChronoUnit.DAYS.between(today, expiryDate);
        return days <= 7 ? "Expiring Soon" : "Active";
    }

    public static LocalDate calculateExpiryDate(LocalDate startDate, String planName) {
        if (startDate == null) startDate = LocalDate.now();
        if (planName == null) planName = "Monthly";
        return switch (planName.trim()) {
            case "Monthly" -> startDate.plusMonths(1);
            case "Quarterly" -> startDate.plusMonths(3);
            case "Yearly" -> startDate.plusYears(1);
            default -> startDate.plusMonths(1);
        };
    }

    public static LocalDate calculateRenewalDate(LocalDate currentExpiry, String planName) {
        LocalDate today = LocalDate.now();
        LocalDate baseDate = (currentExpiry != null && !currentExpiry.isBefore(today)) ? currentExpiry : today;
        return calculateExpiryDate(baseDate, planName);
    }

    public static List<String> validate(String name, String phone, String email, LocalDate joinDate, String plan) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().length() < 2) {
            errors.add("Full name is required (minimum 2 characters).");
        }
        if (phone == null || phone.trim().isEmpty()) {
            errors.add("Phone number is required.");
        } else {
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.length() < 7 || digits.length() > 15) {
                errors.add("Please provide a valid 7 to 15-digit phone number.");
            }
        }
        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                errors.add("Invalid email address format.");
            }
        }
        if (joinDate == null) {
            errors.add("Join date is required.");
        }
        if (plan == null || (!plan.equals("Monthly") && !plan.equals("Quarterly") && !plan.equals("Yearly"))) {
            errors.add("Please select a valid membership plan (Monthly, Quarterly, or Yearly).");
        }
        return errors;
    }

    @Override
    public String toString() {
        return name + " (" + getFormattedId() + " - " + plan + ")";
    }
}
