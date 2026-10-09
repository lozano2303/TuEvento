package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.SeatBlockTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatBlockTranslationJpaRepository extends JpaRepository<SeatBlockTranslationEntity, Integer> {

    @Query("SELECT sbt FROM SeatBlockTranslationEntity sbt WHERE sbt.seatBlockId = :seatBlockId")
    List<SeatBlockTranslationEntity> findBySeatBlockId(@Param("seatBlockId") Integer seatBlockId);

    @Query("SELECT sbt FROM SeatBlockTranslationEntity sbt WHERE sbt.seatBlockId = :seatBlockId AND sbt.languageId = :languageId")
    Optional<SeatBlockTranslationEntity> findBySeatBlockIdAndLanguageId(@Param("seatBlockId") Integer seatBlockId, 
                                                                        @Param("languageId") Integer languageId);

    @Query("SELECT sbt FROM SeatBlockTranslationEntity sbt WHERE sbt.languageId = :languageId")
    List<SeatBlockTranslationEntity> findByLanguageId(@Param("languageId") Integer languageId);

    @Query("SELECT sbt FROM SeatBlockTranslationEntity sbt WHERE sbt.status = :status")
    List<SeatBlockTranslationEntity> findByStatus(@Param("status") String status);

    @Query("SELECT sbt FROM SeatBlockTranslationEntity sbt WHERE sbt.source = :source")
    List<SeatBlockTranslationEntity> findBySource(@Param("source") String source);

    @Query("SELECT sbt FROM SeatBlockTranslationEntity sbt WHERE sbt.seatBlockId = :seatBlockId AND sbt.status = :status")
    List<SeatBlockTranslationEntity> findBySeatBlockIdAndStatus(@Param("seatBlockId") Integer seatBlockId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM SeatBlockTranslationEntity sbt WHERE sbt.seatBlockId = :seatBlockId")
    void deleteBySeatBlockId(@Param("seatBlockId") Integer seatBlockId);

    @Query("SELECT COUNT(sbt) > 0 FROM SeatBlockTranslationEntity sbt WHERE sbt.seatBlockId = :seatBlockId AND sbt.languageId = :languageId")
    boolean existsBySeatBlockIdAndLanguageId(@Param("seatBlockId") Integer seatBlockId, @Param("languageId") Integer languageId);
}