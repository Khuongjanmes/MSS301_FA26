package fu.de190366.userservice.entity;

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
@Table(name = "users", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Nationalized
    @Column(name = "username", nullable = false, length = 30, unique = true)
    private String username;

    @Nationalized
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NVARCHAR)
    @Column(name = "role", nullable = false, length = 15)
    private Role role;

    @Column(name = "rate_per_post", nullable = false, precision = 18, scale = 2)
    private BigDecimal ratePerPost;

    @Column(name = "max_daily_posts", nullable = false)
    private Integer maxDailyPosts;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NVARCHAR)
    @Column(name = "status", nullable = false, length = 15)
    private UserStatus status;
}
