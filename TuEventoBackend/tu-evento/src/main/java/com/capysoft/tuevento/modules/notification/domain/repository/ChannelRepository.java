package com.capysoft.tuevento.modules.notification.domain.repository;

import com.capysoft.tuevento.modules.notification.domain.model.Channel;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository {
    List<Channel> findAllActive();
    Optional<Channel> findByName(String name);
    Channel save(Channel channel);
}
