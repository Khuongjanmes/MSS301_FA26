package fu.de190366.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "room_id", nullable = false)
    private Long id;

    @Nationalized
    @Column(name = "room_number", nullable = false, length = 20, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NVARCHAR)
    @Column(name = "room_type", nullable = false, length = 10)
    private RoomType roomType;

    @Column(name = "price_per_night", nullable = false, precision = 18, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NVARCHAR)
    @Column(name = "status", nullable = false, length = 15)
    private RoomStatus status;
}
