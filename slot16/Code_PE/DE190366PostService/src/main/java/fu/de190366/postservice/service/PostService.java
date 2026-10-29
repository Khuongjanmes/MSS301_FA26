package fu.de190366.postservice.service;

import fu.de190366.postservice.dto.ApiResponse;
import fu.de190366.postservice.dto.PostDTO;
import org.springframework.data.domain.Pageable;

public interface PostService {
    ApiResponse createPost(PostDTO dto);
    ApiResponse updatePost(Long postId, PostDTO dto);
    ApiResponse getPostById(Long postId);
    ApiResponse cancelPost(Long postId);
    ApiResponse getPostList(String advertiserName, String status, Pageable pageable);
}
