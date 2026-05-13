package com.today.fridge.meal.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "health_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @ElementCollection
    @CollectionTable(name = "health_report_advice", joinColumns = @JoinColumn(name = "health_report_id"))
    @Column(name = "advice")
    private List<String> advice;

    @ElementCollection
    @CollectionTable(name = "health_report_meals", joinColumns = @JoinColumn(name = "health_report_id"))
    @Column(name = "meal")
    private List<String> meals;

    @ElementCollection
    @CollectionTable(name = "health_report_videos", joinColumns = @JoinColumn(name = "health_report_id"))
    @Column(name = "video")
    private List<String> videos;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
