package com.today.fridge.post.service;

import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.file.repository.FileAssetRepository;
import com.today.fridge.file.dto.FileAssetDto;
import com.today.fridge.post.dto.PostCreateRequest;
import com.today.fridge.post.dto.PostSummaryResponse;
import com.today.fridge.post.dto.PostUpdateRequest;
import com.today.fridge.post.dto.PostDetailResponse;
import com.today.fridge.post.dto.PostImageDto;
import com.today.fridge.post.entity.Post;
import com.today.fridge.post.entity.PostImage;
import com.today.fridge.post.repository.PostImageRepository;
import com.today.fridge.post.repository.PostRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FileAssetRepository fileAssetRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;

    // ==========================================
    // 1. 게시글 및 이미지 업로드 등록
    // ==========================================
    @Transactional
    public void createPostWithImages(PostCreateRequest request) {
        User author = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid User ID"));

        Post post = new Post();
        post.setAuthorUser(author);
        post.setRecipeId(request.getRecipe()); 
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setLikeCount(0L);
        post.setReportCount(0L);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        Post savedPost = postRepository.save(post);

        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            int sortOrder = 1;
            for (FileAssetDto fileDto : request.getImageFiles()) {
                FileAsset fileAsset = new FileAsset();
                fileAsset.setUploaderUser(author);
                fileAsset.setStorageType("LOCAL");
                fileAsset.setOriginalName(fileDto.getOriginalName());
                fileAsset.setStoredName(fileDto.getUuidName());
                fileAsset.setMimeType(fileDto.getMimeType());
                fileAsset.setFileSize(fileDto.getFileSize());
                fileAsset.setStoragePath(fileDto.getStoragePath());
                fileAsset.setChecksumValue(fileDto.getSha1sum());
                fileAsset.setCreatedAt(LocalDateTime.now());
                
                FileAsset savedFileAsset = fileAssetRepository.save(fileAsset);

                PostImage postImage = new PostImage();
                postImage.setPost(savedPost);
                postImage.setFile(savedFileAsset);
                postImage.setSortOrder(sortOrder++);
                postImage.setCreatedAt(LocalDateTime.now());
                
                postImageRepository.save(postImage);
            }
        }
    }

// ==========================================
    // 2. 커뮤니티 메인: 모든 유저의 최신 게시글 조회 (페이징 적용)
    // ==========================================
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllRecentPosts(pageable);
    }

    // ==========================================
    // 3. 후기 작성 페이지: 특정 유저의 최근 게시글 4개 조회
    // ==========================================
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getUserPosts(Long userId) {
        // PageRequest를 통해 딱 4개까지만 조회하도록 최적화 완료!
        Pageable limit = PageRequest.of(0, 4);
        return postRepository.findRecentPostsByUserId(userId, limit);
    }

    // ==========================================
    // 4. 상세 페이지: 게시글 상세 정보 및 이미지 목록 조회
    // ==========================================
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findPostWithAuthor(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        List<PostImageDto> imageDtos = postImageRepository.findByPost(post).stream()
                .map(img -> new PostImageDto(img.getFile().getStoragePath(), img.getFile().getStoredName()))
                .toList();

        return new PostDetailResponse(
                post.getPostId(), post.getTitle(), post.getContent(),
                post.getAuthorUser().getLoginId(), post.getAuthorUser().getUserId(),
                post.getCreatedAt(), post.getRecipeId(), imageDtos
        );
    }

    // ==========================================
    // 5. 게시글 삭제 로직
    // ==========================================
    @Transactional
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }

    // ==========================================
    // 6. 게시글 수정 로직 (PATCH)
    // ==========================================
    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getRecipe() != null) post.setRecipeId(request.getRecipe());
        post.setUpdatedAt(LocalDateTime.now());

        // 💡 1. 기존 이미지 삭제 및 유지 처리
        List<PostImage> currentImages = postImageRepository.findByPost(post);
        List<String> retainedNames = request.getRetained_images() != null ? request.getRetained_images() : new ArrayList<>();

        int sortOrder = 1;
        for (PostImage pi : currentImages) {
            // 프론트엔드에서 유지하겠다고 보낸 목록에 없는 이미지는 DB에서 매핑 삭제
            if (!retainedNames.contains(pi.getFile().getStoredName())) {
                postImageRepository.delete(pi); 
            } else {
                // 살아남은 이미지는 순서를 1번부터 차례대로 재배치
                pi.setSortOrder(sortOrder++); 
            }
        }

        // 💡 2. 새 이미지 추가
        if (request.getImage_files() != null && !request.getImage_files().isEmpty()) {
            for (FileAssetDto fileDto : request.getImage_files()) {
                FileAsset fileAsset = new FileAsset();
                fileAsset.setUploaderUser(post.getAuthorUser());
                fileAsset.setStorageType("LOCAL");
                fileAsset.setOriginalName(fileDto.getOriginalName());
                fileAsset.setStoredName(fileDto.getUuidName());
                fileAsset.setMimeType(fileDto.getMimeType());
                fileAsset.setFileSize(fileDto.getFileSize());
                fileAsset.setStoragePath(fileDto.getStoragePath());
                fileAsset.setChecksumValue(fileDto.getSha1sum());
                fileAsset.setCreatedAt(LocalDateTime.now());
                FileAsset savedFileAsset = fileAssetRepository.save(fileAsset);

                PostImage postImage = new PostImage();
                postImage.setPost(post);
                postImage.setFile(savedFileAsset);
                // 기존 살아남은 이미지들의 다음 순서(sortOrder)를 이어받아 저장
                postImage.setSortOrder(sortOrder++);
                postImage.setCreatedAt(LocalDateTime.now());
                postImageRepository.save(postImage);
            }
        }
    }
}