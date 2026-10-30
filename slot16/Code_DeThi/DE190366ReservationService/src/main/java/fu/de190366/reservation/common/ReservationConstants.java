package fu.de190366.reservation.common;

public final class ReservationConstants {
    private ReservationConstants() {}

    public static final String STATUS_REGEX = "CONFIRMED|CHECKED_IN|CHECKED_OUT|CANCELLED";

    // Room status coming from RoomService
    public static final String ROOM_AVAILABLE = "AVAILABLE";
}
