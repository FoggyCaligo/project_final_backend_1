package com.today.fridge.post.service;

import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.file.repository.FileAssetRepository;
import com.today.fridge.file.dto.FileAssetDto;
import com.today.fridge.post.dto.PostCreateRequest;
import com.today.fridge.post.entity.Post;
import com.today.fridge.post.entity.PostImage;
import com.today.fridge.post.repository.PostImageRepository;
import com.today.fridge.post.repository.PostRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FileAssetRepository fileAssetRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository; // User 엔티티 조회를 위해 필요

    @Transactional
    public void createPostWithImages(PostCreateRequest request) {
        // 1. 유저 조회 (실제로는 Spring Security Context 등에서 가져오는 것이 좋습니다)
        User author = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid User ID"));

        // 2. Post 엔티티 저장
        Post post = new Post();
        post.setAuthorUser(author);
        post.setRecipeId(request.getRecipe()); // 프론트에서 넘어온 레시피 ID 세팅
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setLikeCount(0L);
        post.setReportCount(0L);
        post.setViewCount(0L);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        Post savedPost = postRepository.save(post);

        // 3. 파일 메타데이터가 있다면 FileAsset 및 PostImage 저장
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            int sortOrder = 1;
            
            for (FileAssetDto fileDto : request.getImageFiles()) {
                // 3-1. FileAsset 저장
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

                // 3-2. PostImage 매핑 테이블 저장
                PostImage postImage = new PostImage();
                postImage.setPost(savedPost);
                postImage.setFile(savedFileAsset);
                postImage.setSortOrder(sortOrder++);
                postImage.setCreatedAt(LocalDateTime.now());
                
                postImageRepository.save(postImage);
            }
        }
    }
}