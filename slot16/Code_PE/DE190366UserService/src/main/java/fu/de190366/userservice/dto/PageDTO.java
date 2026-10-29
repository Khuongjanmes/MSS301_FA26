package fu.de190366.userservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PageDTO {
    private int size;
    private int page;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;
    private Object content;   // List<UserDTO>

    public PageDTO(int size, int page, int totalPages, long totalElements,
                   boolean first, boolean last, Object content) {
        this.size = size;
        this.page = page;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.first = first;
        this.last = last;
        this.content = content;
    }
}
