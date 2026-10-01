package com.cinekeep.list;

import com.cinekeep.media.MediaItem;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "user_list_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_list_items_list_media",
                columnNames = {"user_list_id", "media_item_id"}
        )
)
public class UserListItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_list_id", nullable = false)
    private UserList userList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_item_id", nullable = false)
    private MediaItem mediaItem;

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected UserListItem() {
    }

    public UserListItem(UserList userList, MediaItem mediaItem, String comment) {
        this.userList = userList;
        this.mediaItem = mediaItem;
        this.comment = comment;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public UserList getUserList() {
        return userList;
    }

    public MediaItem getMediaItem() {
        return mediaItem;
    }

    public String getComment() {
        return comment;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
