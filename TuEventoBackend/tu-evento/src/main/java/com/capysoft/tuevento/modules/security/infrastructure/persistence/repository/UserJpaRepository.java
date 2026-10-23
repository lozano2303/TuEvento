package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role WHERE u.alias = :alias")
    Optional<UserEntity> findByAlias(@Param("alias") String alias);
    
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.alias = :alias")
    boolean existsByAlias(@Param("alias") String alias);

    @Modifying
    @Query("UPDATE UserEntity u SET u.userStatus = (SELECT s FROM UserStatusEntity s WHERE s.code = :statusCode) WHERE u.userId = :userId")
    void updateStatusByUserId(@Param("userId") Integer userId, @Param("statusCode") String statusCode);

    @Query("SELECT u.userStatus.code FROM UserEntity u WHERE u.userId = :userId")
    String findStatusCodeByUserId(@Param("userId") Integer userId);

    /** Most recent N registered users — used for admin activity feed. */
    @Query("SELECT u FROM UserEntity u ORDER BY u.createdAt DESC")
    List<UserEntity> findTopByOrderByCreatedAtDesc(Pageable pageable);

    /** All users currently holding a specific role code. Eagerly fetches role + userStatus to avoid LazyInitializationException. */
    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role r JOIN FETCH u.userStatus WHERE r.code = :roleCode")
    List<UserEntity> findAllByRoleCode(@Param("roleCode") String roleCode);

    /** Count events for a given user — avoids loading the full event list just for a count. */
    @Query("SELECT COUNT(e) FROM com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventEntity e WHERE e.userId = :userId")
    long countEventsByUserId(@Param("userId") Long userId);

    /** Count published events for a given user. */
    @Query(value = "SELECT COUNT(*) FROM event WHERE user_id = :userId AND status = 'PUBLISHED'", nativeQuery = true)
    long countPublishedEventsByUserId(@Param("userId") Long userId);
}
