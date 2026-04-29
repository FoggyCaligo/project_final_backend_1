package com.today.fridge.post.repository;

import com.today.fridge.post.dto.PostSummaryResponse;
import com.today.fridge.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional; // 💡 Optional을 사용하기 위한 필수 import 추가

public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 유저의 게시글 조회 (💡 DTO 생성자에 맞게 p.postId 추가)
    @Query("SELECT new com.today.fridge.post.dto.PostSummaryResponse(" +
           "p.postId, p.title, u.loginId, p.createdAt, p.content, f.storagePath, f.storedName) " +
           "FROM Post p " +
           "JOIN p.authorUser u " +
           "LEFT JOIN PostImage pi ON pi.post.postId = p.postId AND pi.sortOrder = 1 " + // 첫 번째 이미지만 가져옴
           "LEFT JOIN pi.file f " +
           "WHERE u.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    List<PostSummaryResponse> findRecentPostsByUserId(@Param("userId") Long userId);

    // 모든 유저의 게시글을 최신순으로 조회 (💡 DTO 생성자에 맞게 p.postId 추가)
    @Query("SELECT new com.today.fridge.post.dto.PostSummaryResponse(" +
           "p.postId, p.title, u.loginId, p.createdAt, p.content, f.storagePath, f.storedName) " +
           "FROM Post p " +
           "JOIN p.authorUser u " +
           "LEFT JOIN PostImage pi ON pi.post.postId = p.postId AND pi.sortOrder = 1 " +
           "LEFT JOIN pi.file f " +
           "ORDER BY p.createdAt DESC")
    List<PostSummaryResponse> findAllRecentPosts();

    // 상세 조회를 위한 쿼리
    @Query("SELECT p FROM Post p JOIN FETCH p.authorUser WHERE p.postId = :postId")
    Optional<Post> findPostWithAuthor(@Param("postId") Long postId);
}