package com.cinekeep.rating;

import com.cinekeep.media.MediaItem;
import com.cinekeep.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ratings_user_media",
                columnNames = {"user_id", "media_item_id"}
        )
)
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_item_id", nullable = false)
    private MediaItem mediaItem;

    @Column(nullable = false)
    private Double value;

    @CreationTimestamp
    @Column(name = "rated_at", nullable = false, updatable = false)
    private Instant ratedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Rating() {
    }

    public Rating(User user, MediaItem mediaItem, Double value) {
        this.user = user;
        this.mediaItem = mediaItem;
        this.value = value;
    }

    public void updateValue(Double value) {
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MediaItem getMediaItem() {
        return mediaItem;
    }

    public Double getValue() {
        return value;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
