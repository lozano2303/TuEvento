package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountActivationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SpringDataAccountActivationRepository extends JpaRepository<AccountActivationEntity, Integer> {

    // Métodos existentes
    Optional<AccountActivationEntity> findByUser_UserId(Integer userId);

    Optional<AccountActivationEntity> findByActivationCode(String activationCode);

    @Modifying
    @Transactional
    void deleteByUser_UserId(Integer userId);

    // ✅ NUEVOS MÉTODOS para el reenvío de códigos
    Optional<AccountActivationEntity> findByUser_UserIdAndActivationCodeAndActivatedFalse(Integer userId, String activationCode);

    Optional<AccountActivationEntity> findTopByUser_UserIdAndActivatedFalseOrderByCreatedAtDesc(Integer userId);
}