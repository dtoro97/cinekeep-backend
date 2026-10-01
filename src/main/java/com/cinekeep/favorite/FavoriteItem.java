package com.cinekeep.favorite;

import com.cinekeep.media.MediaItem;
import com.cinekeep.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "favorite_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_items_user_media",
                columnNames = {"user_id", "media_item_id"}
        )
)
public class FavoriteItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_item_id", nullable = false)
    private MediaItem mediaItem;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected FavoriteItem() {
    }

    public FavoriteItem(User user, MediaItem mediaItem) {
        this.user = user;
        this.mediaItem = mediaItem;
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

    public Instant getAddedAt() {
        return addedAt;
    }
}
