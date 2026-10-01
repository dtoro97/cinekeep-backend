package com.cinekeep.list;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserListRepository extends JpaRepository<UserList, Long> {
    @EntityGraph(attributePaths = "coverMediaItem")
    Page<UserList> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "coverMediaItem")
    Optional<UserList> findByIdAndUser_Id(Long id, Long userId);
}
