package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.application.dto.response.AdminUserResponse;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role WHERE u.alias = :alias")
    Optional<UserEntity> findByAlias(@Param("alias") String alias);

    /** Eager load of role + userStatus by ID — use this instead of findById() when the entity will be modified or mapped to domain. */
    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role JOIN FETCH u.userStatus WHERE u.userId = :userId")
    Optional<UserEntity> findByIdEager(@Param("userId") Integer userId);
    
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.alias = :alias")
    boolean existsByAlias(@Param("alias") String alias);

    @Modifying
    @Query("UPDATE UserEntity u SET u.userStatus = (SELECT s FROM UserStatusEntity s WHERE s.code = :statusCode) WHERE u.userId = :userId")
    void updateStatusByUserId(@Param("userId") Integer userId, @Param("statusCode") String statusCode);

    @Query("SELECT u.userStatus.code FROM UserEntity u WHERE u.userId = :userId")
    String findStatusCodeByUserId(@Param("userId") Integer userId);

    @Query("SELECT u.role.code FROM UserEntity u WHERE u.userId = :userId")
    String findRoleCodeByUserId(@Param("userId") Integer userId);

    /** Most recent N registered users — used for admin activity feed. */
    @Query("SELECT u FROM UserEntity u ORDER BY u.createdAt DESC")
    List<UserEntity> findTopByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Listado de usuarios para el panel admin con nombre (profile) y email (login_credentials).
     * {@code q} ya llega en minúsculas con comodines ("%texto%"); "" desactiva cada filtro.
     */
    @Query("""
            SELECT u.userId AS userId, u.alias AS alias, p.fullName AS fullName, c.email AS email,
                   r.code AS role, s.code AS status, u.createdAt AS createdAt
            FROM UserEntity u
            JOIN u.role r
            JOIN u.userStatus s
            LEFT JOIN ProfileEntity p ON p.userId = u.userId
            LEFT JOIN LoginCredentialsEntity c ON c.user = u
            WHERE (:role = '' OR r.code = :role)
              AND (:q = '' OR LOWER(u.alias) LIKE :q OR LOWER(p.fullName) LIKE :q OR LOWER(c.email) LIKE :q)
            ORDER BY u.createdAt DESC
            """)
    List<AdminUserResponse> searchForAdmin(@Param("q") String q, @Param("role") String role, Pageable pageable);

    /**
     * Datos públicos de varios usuarios en una sola consulta: alias, email (login_credentials) y
     * nombre, foto e idioma (profile). Un usuario sin perfil o sin credenciales sale igual, con
     * esos campos en null.
     */
    @Query("""
            SELECT new com.capysoft.tuevento.modules.security.application.dto.UserSummary(
                       u.userId, u.alias, c.email, p.fullName, p.storedFileId, p.preferredLanguageId)
            FROM UserEntity u
            LEFT JOIN ProfileEntity p ON p.userId = u.userId
            LEFT JOIN LoginCredentialsEntity c ON c.user = u
            WHERE u.userId IN :userIds
            """)
    List<UserSummary> findSummariesByUserIds(@Param("userIds") Collection<Integer> userIds);

    /** IDs mayores que {@code afterUserId} en orden ascendente (recorrido por cursor). */
    @Query("SELECT u.userId FROM UserEntity u WHERE u.userId > :afterUserId ORDER BY u.userId ASC")
    List<Integer> findUserIdsAfter(@Param("afterUserId") Integer afterUserId, Pageable pageable);

}
