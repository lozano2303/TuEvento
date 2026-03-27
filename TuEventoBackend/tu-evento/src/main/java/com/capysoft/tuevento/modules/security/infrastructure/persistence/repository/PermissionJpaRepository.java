package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, Integer> {

    Optional<PermissionEntity> findByCode(String code);

    @Query("SELECT p FROM PermissionEntity p JOIN p.roles r WHERE r.roleId = :roleId")
    List<PermissionEntity> findByRoleId(@Param("roleId") Integer roleId);
}
