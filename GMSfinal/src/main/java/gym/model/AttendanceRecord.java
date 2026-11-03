package gym.model;

import java.time.LocalDate;

public record AttendanceRecord(
    int id,
    int memberId,
    String memberName,
    String memberPhone,
    LocalDate checkInDate,
    String checkInTime,
    String notes
) {}

