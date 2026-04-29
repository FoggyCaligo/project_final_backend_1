package com.today.fridge.post.repository;

import com.today.fridge.post.dto.PostSummaryResponse;
import com.today.fridge.post.entity.Post;
import org.springframework.data.domain.Page; // 💡 Page import 추가
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 유저의 게시글 조회 (파라미터에 Pageable 적용 완료)
    @Query("SELECT new com.today.fridge.post.dto.PostSummaryResponse(" +
           "p.postId, p.title, u.loginId, p.createdAt, p.content, f.storagePath, f.storedName) " +
           "FROM Post p " +
           "JOIN p.authorUser u " +
           "LEFT JOIN PostImage pi ON pi.post.postId = p.postId AND pi.sortOrder = 1 " +
           "LEFT JOIN pi.file f " +
           "WHERE u.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    List<PostSummaryResponse> findRecentPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    // 💡 모든 유저의 게시글을 조회 (List -> Page 로 반환 타입 변경 및 Pageable 파라미터 추가)
    @Query("SELECT new com.today.fridge.post.dto.PostSummaryResponse(" +
           "p.postId, p.title, u.loginId, p.createdAt, p.content, f.storagePath, f.storedName) " +
           "FROM Post p " +
           "JOIN p.authorUser u " +
           "LEFT JOIN PostImage pi ON pi.post.postId = p.postId AND pi.sortOrder = 1 " +
           "LEFT JOIN pi.file f " +
           "ORDER BY p.createdAt DESC")
    Page<PostSummaryResponse> findAllRecentPosts(Pageable pageable);

    // 상세 조회를 위한 쿼리
    @Query("SELECT p FROM Post p JOIN FETCH p.authorUser WHERE p.postId = :postId")
    Optional<Post> findPostWithAuthor(@Param("postId") Long postId);
}