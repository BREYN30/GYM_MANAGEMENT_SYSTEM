package gym.model;

import java.time.LocalDate;

public record RenewalRecord(
    int id,
    int memberId,
    String memberName,
    String plan,
    LocalDate previousExpiry,
    LocalDate newExpiry,
    LocalDate renewalDate,
    double amount
) {}

