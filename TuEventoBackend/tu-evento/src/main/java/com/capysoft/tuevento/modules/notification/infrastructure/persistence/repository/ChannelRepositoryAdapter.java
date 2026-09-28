package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.domain.model.Channel;
import com.capysoft.tuevento.modules.notification.domain.repository.ChannelRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.ChannelEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChannelRepositoryAdapter implements ChannelRepository {

    private final JpaChannelRepository jpaChannelRepository;

    @Override
    public Optional<Channel> findByName(String name) {
        return jpaChannelRepository.findByName(name)
                .map(this::toDomain);
    }

    @Override
    public List<Channel> findAllActive() {
        return jpaChannelRepository.findAllActive().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Channel save(Channel channel) {
        ChannelEntity entity = toEntity(channel);
        ChannelEntity saved = jpaChannelRepository.save(entity);
        return toDomain(saved);
    }

    private Channel toDomain(ChannelEntity entity) {
        return Channel.builder()
                .channelId(entity.getChannelId())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .config(entity.getConfig())
                .build();
    }

    private ChannelEntity toEntity(Channel domain) {
        return ChannelEntity.builder()
                .channelId(domain.getChannelId())
                .name(domain.getName())
                .description(domain.getDescription())
                .active(domain.isActive())
                .config(domain.getConfig())
                .build();
    }
}