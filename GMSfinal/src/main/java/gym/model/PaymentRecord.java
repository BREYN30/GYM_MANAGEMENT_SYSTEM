package gym.model;

import java.time.LocalDate;

public record PaymentRecord(
    int id,
    String txnRef,
    int memberId,
    String memberName,
    String plan,
    double amount,
    String paymentMethod,
    LocalDate paymentDate,
    String notes
) {}

