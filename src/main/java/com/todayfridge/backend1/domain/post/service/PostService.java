package com.todayfridge.backend1.domain.post.service;

import com.todayfridge.backend1.domain.file.entity.StoredFile;
import com.todayfridge.backend1.domain.file.service.FileStorageService;
import com.todayfridge.backend1.domain.file.service.FileValidationService;
import com.todayfridge.backend1.domain.post.entity.Post;
import com.todayfridge.backend1.domain.post.entity.PostImage;
import com.todayfridge.backend1.domain.post.entity.PostLike;
import com.todayfridge.backend1.domain.post.repository.PostImageRepository;
import com.todayfridge.backend1.domain.post.repository.PostLikeRepository;
import com.todayfridge.backend1.domain.post.repository.PostRepository;
import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.domain.user.repository.UserRepository;
import com.todayfridge.backend1.global.error.BusinessException;
import com.todayfridge.backend1.global.error.ErrorCode;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;

    public PostService(PostRepository postRepository, PostImageRepository postImageRepository,
                       PostLikeRepository postLikeRepository, UserRepository userRepository,
                       FileStorageService fileStorageService, FileValidationService fileValidationService) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.postLikeRepository = postLikeRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getId).reversed())
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long postId) {
        Post post = getPost(postId);
        post.increaseViewCount();
        Map<String, Object> data = new LinkedHashMap<>(summary(post));
        data.put("content", post.getContent());
        data.put("images", postImageRepository.findByPost_Id(postId).stream().map(img -> Map.of(
                "storedFileId", img.getStoredFile().getId(),
                "url", img.getStoredFile().getStoragePath(),
                "originalName", img.getStoredFile().getOriginalName()
        )).toList());
        return data;
    }

    public Map<String, Object> create(Long userId, String title, String content, List<MultipartFile> images) {
        User author = getUser(userId);
        Post post = postRepository.save(new Post(author, title, content));
        fileValidationService.validatePostImages(images);
        if (images != null) {
            for (MultipartFile image : images) {
                StoredFile storedFile = fileStorageService.store(author, image, "posts");
                postImageRepository.save(new PostImage(post, storedFile));
            }
        }
        return summary(post);
    }

    public Map<String, Object> update(Long userId, Long postId, String title, String content) {
        Post post = getOwnedPost(userId, postId);
        post.update(title, content);
        return summary(post);
    }

    public void delete(Long userId, Long postId) {
        postRepository.delete(getOwnedPost(userId, postId));
    }

    public void like(Long userId, Long postId) {
        if (postLikeRepository.findByPost_IdAndUser_Id(postId, userId).isPresent()) return;
        Post post = getPost(postId);
        postLikeRepository.save(new PostLike(post, getUser(userId)));
        post.increaseLikeCount();
    }

    public void unlike(Long userId, Long postId) {
        postLikeRepository.findByPost_IdAndUser_Id(postId, userId).ifPresent(like -> {
            postLikeRepository.delete(like);
            like.getPost().decreaseLikeCount();
        });
    }

    private Post getOwnedPost(Long userId, Long postId) {
        Post post = getPost(postId);
        if (!Objects.equals(post.getAuthor().getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "게시글 수정 권한이 없습니다.");
        }
        return post;
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Map<String, Object> summary(Post post) {
        return Map.of(
                "postId", post.getId(),
                "title", post.getTitle(),
                "authorUserId", post.getAuthor().getId(),
                "authorNickname", post.getAuthor().getNickname(),
                "likeCount", post.getLikeCount(),
                "viewCount", post.getViewCount(),
                "createdAt", post.getCreatedAt()
        );
    }
}
