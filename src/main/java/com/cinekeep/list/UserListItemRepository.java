package com.cinekeep.list;

import com.cinekeep.media.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserListItemRepository extends JpaRepository<UserListItem, Long> {
    @EntityGraph(attributePaths = "mediaItem")
    Page<UserListItem> findByUserList_Id(Long listId, Pageable pageable);

    @EntityGraph(attributePaths = "mediaItem")
    Optional<UserListItem> findByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
            Long listId,
            MediaType mediaType,
            Integer tmdbId
    );

    boolean existsByUserList_IdAndMediaItem_MediaTypeAndMediaItem_TmdbId(
            Long listId,
            MediaType mediaType,
            Integer tmdbId
    );

    long countByUserList_Id(Long listId);

    @EntityGraph(attributePaths = "mediaItem")
    Optional<UserListItem> findFirstByUserList_IdOrderByIdDesc(Long listId);

    @Query("""
            select item.userList.id as listId, count(item) as itemCount
            from UserListItem item
            where item.userList.id in :listIds
            group by item.userList.id
            """)
    List<ListItemCount> countByListIds(@Param("listIds") Collection<Long> listIds);

    @Query("""
            select item
            from UserListItem item
            join fetch item.mediaItem
            where item.userList.id in :listIds
              and item.id = (
                  select max(latest.id)
                  from UserListItem latest
                  where latest.userList = item.userList
              )
            """)
    List<UserListItem> findLatestItemsByListIds(@Param("listIds") Collection<Long> listIds);

    @Query("""
            select item.userList.id
            from UserListItem item
            where item.userList.user.id = :userId
              and item.mediaItem.mediaType = :mediaType
              and item.mediaItem.tmdbId = :tmdbId
            order by item.userList.id
            """)
    List<Long> findListIdsContainingMedia(
            @Param("userId") Long userId,
            @Param("mediaType") MediaType mediaType,
            @Param("tmdbId") Integer tmdbId
    );

    @Modifying
    @Query("delete from UserListItem item where item.userList.id = :listId")
    void deleteAllByListId(@Param("listId") Long listId);
}
