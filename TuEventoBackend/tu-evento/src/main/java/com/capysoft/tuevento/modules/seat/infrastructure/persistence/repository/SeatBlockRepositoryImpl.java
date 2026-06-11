package com.capysoft.tuevento.modules.seat.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.seat.domain.model.SeatBlock;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatBlockRepository;
import com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity.SeatBlockEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SeatBlockRepositoryImpl implements SeatBlockRepository {

    private final SeatBlockJpaRepository jpaRepository;

    @Override
    public List<SeatBlock> findAllByEventSectionId(Integer eventSectionId) {
        return jpaRepository.findAllByEventSectionId(eventSectionId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SeatBlock> findById(Integer seatBlockId) {
        return jpaRepository.findById(seatBlockId).map(this::toDomain);
    }

    @Override
    public boolean existsByEventSectionIdAndName(Integer eventSectionId, String name) {
        return jpaRepository.existsByEventSectionIdAndName(eventSectionId, name);
    }

    @Override
    public SeatBlock save(SeatBlock seatBlock) {
        SeatBlockEntity entity = SeatBlockEntity.builder()
                .seatBlockId(seatBlock.getSeatBlockId())
                .eventSectionId(seatBlock.getEventSectionId())
                .name(seatBlock.getName())
                .capacity(seatBlock.getCapacity())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Integer seatBlockId) {
        jpaRepository.deleteById(seatBlockId);
    }

    private SeatBlock toDomain(SeatBlockEntity entity) {
        return SeatBlock.builder()
                .seatBlockId(entity.getSeatBlockId())
                .eventSectionId(entity.getEventSectionId())
                .name(entity.getName())
                .capacity(entity.getCapacity())
                .build();
    }
}
