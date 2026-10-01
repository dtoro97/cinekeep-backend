package com.cinekeep.rating;

import com.cinekeep.media.MediaItem;
import com.cinekeep.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "episode_ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_episode_ratings_user_episode",
                columnNames = {"user_id", "series_item_id", "season_number", "episode_number"}
        )
)
public class EpisodeRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_item_id", nullable = false)
    private MediaItem seriesItem;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(name = "episode_name", nullable = false)
    private String episodeName;

    @Column(name = "still_path")
    private String stillPath;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(nullable = false)
    private Double value;

    @CreationTimestamp
    @Column(name = "rated_at", nullable = false, updatable = false)
    private Instant ratedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EpisodeRating() {
    }

    public EpisodeRating(
            User user,
            MediaItem seriesItem,
            Integer seasonNumber,
            Integer episodeNumber,
            String episodeName,
            String stillPath,
            LocalDate airDate,
            Double value
    ) {
        this.user = user;
        this.seriesItem = seriesItem;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.episodeName = episodeName;
        this.stillPath = stillPath;
        this.airDate = airDate;
        this.value = value;
    }

    public void update(String episodeName, String stillPath, LocalDate airDate, Double value) {
        this.episodeName = episodeName;
        this.stillPath = stillPath;
        this.airDate = airDate;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MediaItem getSeriesItem() {
        return seriesItem;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public String getEpisodeName() {
        return episodeName;
    }

    public String getStillPath() {
        return stillPath;
    }

    public LocalDate getAirDate() {
        return airDate;
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
