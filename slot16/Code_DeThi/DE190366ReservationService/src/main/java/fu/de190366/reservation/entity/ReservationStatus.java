package fu.de190366.reservation.entity;

/** Trang thai dat phong. Luu vao DB duoi dang chuoi (nvarchar)
 * qua @Enumerated(EnumType.STRING). */
public enum ReservationStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
