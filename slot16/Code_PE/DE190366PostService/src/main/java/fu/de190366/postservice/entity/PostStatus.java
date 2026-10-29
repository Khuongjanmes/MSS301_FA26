package fu.de190366.postservice.entity;

/** Trang thai chien dich bai dang. Luu vao DB duoi dang chuoi (nvarchar)
 * qua @Enumerated(EnumType.STRING). */
public enum PostStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED,
    CANCELLED
}
