package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventCommentReply;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventCommentReplyEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventCommentReplyEntityMapper {

    EventCommentReply toDomain(EventCommentReplyEntity entity);
    EventCommentReplyEntity toEntity(EventCommentReply domain);
}
