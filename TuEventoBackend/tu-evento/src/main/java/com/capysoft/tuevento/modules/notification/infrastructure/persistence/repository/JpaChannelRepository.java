package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.ChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaChannelRepository extends JpaRepository<ChannelEntity, Long> {
    
    Optional<ChannelEntity> findByName(String name);
    
    @Query("SELECT c FROM ChannelEntity c WHERE c.active = true ORDER BY c.name")
    List<ChannelEntity> findAllActive();
}