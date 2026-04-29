package com.today.fridge.post.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class PostSummaryResponse {
    private Long postId;
    private String title;
    private String author; // User 테이블의 login_id
    private LocalDateTime date;
    private String desc;
    private String storagePath;
    private String storedName;
    
    public PostSummaryResponse(Long postId, String title, String author, LocalDateTime date, String desc, String storagePath, String storedName) {
        this.postId = postId;
        this.title = title;
        this.author = author;
        this.date = date;
        this.desc = desc;
        this.storagePath = storagePath;
        this.storedName = storedName;
    }
}