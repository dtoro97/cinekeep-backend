package com.cinekeep.media;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "media_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_media_items_media_type_tmdb_id",
                columnNames = {"media_type", "tmdb_id"}
        )
)
public class MediaItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private String title;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "backdrop_path")
    private String backdropPath;

    @Column(length = 2000)
    private String overview;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "vote_average")
    private Double voteAverage;

    @Column(name = "vote_count")
    private Integer voteCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MediaItem() {
    }

    public MediaItem(
            Integer tmdbId,
            MediaType mediaType,
            String title,
            String posterPath,
            String backdropPath,
            String overview,
            LocalDate releaseDate,
            Double voteAverage,
            Integer voteCount
    ) {
        this.tmdbId = tmdbId;
        this.mediaType = mediaType;
        this.title = title;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
    }

    public void mergeSnapshot(
            String title,
            String posterPath,
            String backdropPath,
            String overview,
            LocalDate releaseDate,
            Double voteAverage,
            Integer voteCount
    ) {
        this.title = newValueOrCurrent(title, this.title);
        this.posterPath = newValueOrCurrent(posterPath, this.posterPath);
        this.backdropPath = newValueOrCurrent(backdropPath, this.backdropPath);
        this.overview = newValueOrCurrent(overview, this.overview);
        this.releaseDate = newValueOrCurrent(releaseDate, this.releaseDate);
        this.voteAverage = newValueOrCurrent(voteAverage, this.voteAverage);
        this.voteCount = newValueOrCurrent(voteCount, this.voteCount);
    }

    private static <T> T newValueOrCurrent(T newValue, T currentValue) {
        return newValue != null ? newValue : currentValue;
    }

    public Long getId() {
        return id;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getTitle() {
        return title;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public String getOverview() {
        return overview;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public Double getVoteAverage() {
        return voteAverage;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
