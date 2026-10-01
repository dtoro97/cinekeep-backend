package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
import com.cinekeep.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_lists")
public class UserList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_by", nullable = false, length = 30)
    private ListSortBy sortBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_media_item_id")
    private MediaItem coverMediaItem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserList() {
    }

    public UserList(User user, String name, String description, boolean isPublic, ListSortBy sortBy) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.sortBy = sortBy;
    }

    public void update(String name, String description, boolean isPublic, ListSortBy sortBy, MediaItem coverMediaItem) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.sortBy = sortBy;
        this.coverMediaItem = coverMediaItem;
    }

    public boolean hasCover(MediaItem mediaItem) {
        return coverMediaItem != null && coverMediaItem.getId().equals(mediaItem.getId());
    }

    public void clearCover() {
        this.coverMediaItem = null;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public ListSortBy getSortBy() {
        return sortBy;
    }

    public MediaItem getCoverMediaItem() {
        return coverMediaItem;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
