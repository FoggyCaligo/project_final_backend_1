package com.todayfridge.backend1.domain.post.entity;

import com.todayfridge.backend1.domain.file.entity.StoredFile;
import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "post_images")
public class PostImage extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "post_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;
    @JoinColumn(name = "stored_file_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private StoredFile storedFile;

    protected PostImage() {}
    public PostImage(Post post, StoredFile storedFile) { this.post = post; this.storedFile = storedFile; }
    public StoredFile getStoredFile() { return storedFile; }
}
