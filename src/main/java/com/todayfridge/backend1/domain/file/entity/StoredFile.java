package com.todayfridge.backend1.domain.file.entity;

import com.todayfridge.backend1.domain.user.entity.User;
import com.todayfridge.backend1.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "stored_files")
public class StoredFile extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "owner_user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User ownerUser;
    @Column(nullable = false, length = 255)
    private String originalName;
    @Column(nullable = false, unique = true, length = 255)
    private String storedName;
    @Column(nullable = false, length = 120)
    private String mimeType;
    @Column(nullable = false)
    private long sizeBytes;
    @Column(nullable = false, length = 500)
    private String storagePath;

    protected StoredFile() {}
    public StoredFile(User ownerUser, String originalName, String storedName, String mimeType, long sizeBytes, String storagePath) {
        this.ownerUser = ownerUser;
        this.originalName = originalName;
        this.storedName = storedName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
    }

    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getStoragePath() { return storagePath; }
}
