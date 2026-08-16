package com.capysoft.tuevento.modules.event.application.usecase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.event.application.dto.request.SaveEventLayoutRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventLayoutResponse;
import com.capysoft.tuevento.modules.event.application.port.in.SaveEventLayoutUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventLayout;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventLayoutRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatBlock;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatBlockRepository;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveEventLayoutService implements SaveEventLayoutUseCase {

    private final EventRepository eventRepository;
    private final EventLayoutRepository eventLayoutRepository;
    private final SeatBlockRepository seatBlockRepository;
    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EventLayoutResponse execute(Long eventId, SaveEventLayoutRequest request, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (!event.getUserId().equals(userId)) {
            throw new BusinessException("EVENT_ACCESS_DENIED",
                    "User " + userId + " does not own event " + eventId);
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("EVENT_LAYOUT_EDIT_NOT_ALLOWED",
                    "Layout can only be modified when the event is in DRAFT status. Current status: " + event.getStatus());
        }

        Optional<EventLayout> existing = eventLayoutRepository.findByEventId(eventId);

        EventLayout layout = existing
                .map(l -> EventLayout.builder()
                        .eventLayoutId(l.getEventLayoutId())
                        .eventId(l.getEventId())
                        .layoutData(request.getLayoutData())
                        .build())
                .orElseGet(() -> EventLayout.builder()
                        .eventId(eventId)
                        .layoutData(request.getLayoutData())
                        .build());

        EventLayout saved = eventLayoutRepository.save(layout);

        // Generar automáticamente seat_block y seat a partir del layout
        regenerateSeatsFromLayout(saved.getLayoutData());

        return EventLayoutResponse.builder()
                .eventLayoutId(saved.getEventLayoutId())
                .eventId(saved.getEventId())
                .layoutData(saved.getLayoutData())
                .build();
    }

    /**
     * Parsea el layoutData JSON y regenera los registros de seat_block y seat
     * para cada sección que tenga backendSectionId y seatLayout definido.
     * 
     * Estrategia: por cada sección, borra las sillas existentes y las regenera
     * desde cero según el seatLayout actual. Esto garantiza sincronización
     * perfecta entre el layout visual y las sillas funcionales.
     * 
     * Corre en la misma transacción que guarda el EventLayout — si algo falla,
     * se revierten ambos cambios.
     */
    private void regenerateSeatsFromLayout(String layoutData) {
        try {
            JsonNode root = objectMapper.readTree(layoutData);
            JsonNode elementsNode = root.get("elements");
            
            if (elementsNode == null || !elementsNode.isArray()) {
                log.warn("[SaveEventLayout] layoutData no contiene 'elements' array válido");
                return;
            }

            int sectionsProcessed = 0;
            int seatsGenerated = 0;

            for (JsonNode elementNode : elementsNode) {
                String type = elementNode.has("type") ? elementNode.get("type").asText() : null;
                
                if (!"section".equals(type)) {
                    continue; // Solo procesar elementos tipo 'section'
                }

                JsonNode backendSectionIdNode = elementNode.get("backendSectionId");
                JsonNode seatLayoutNode = elementNode.get("seatLayout");

                if (backendSectionIdNode == null || backendSectionIdNode.isNull() || 
                    seatLayoutNode == null || seatLayoutNode.isNull()) {
                    continue; // Saltar secciones sin backendSectionId o sin seatLayout
                }

                Integer eventSectionId = backendSectionIdNode.asInt();
                int targetSeats = seatLayoutNode.has("targetSeats") ? seatLayoutNode.get("targetSeats").asInt() : 0;
                int rows = seatLayoutNode.has("rows") ? seatLayoutNode.get("rows").asInt() : 1;
                int seatsPerRow = seatLayoutNode.has("seatsPerRow") ? seatLayoutNode.get("seatsPerRow").asInt() : 1;

                if (targetSeats <= 0) {
                    log.debug("[SaveEventLayout] Sección {} tiene targetSeats={}, saltando generación", 
                            eventSectionId, targetSeats);
                    continue;
                }

                // Validación defensiva: verificar que ninguna silla existente esté reservada/vendida
                List<Seat> existingSeats = seatRepository.findAllByEventSectionId(eventSectionId);
                boolean hasNonAvailableSeats = existingSeats.stream()
                        .anyMatch(seat -> seat.getStatus() != SeatStatus.AVAILABLE);

                if (hasNonAvailableSeats) {
                    throw new BusinessException("SEAT_REGENERATION_CONFLICT",
                            "No se puede regenerar las sillas de la sección " + eventSectionId + 
                            " porque algunas ya tienen reservas o ventas. " +
                            "Esto no debería ocurrir en estado DRAFT.");
                }

                // Borrar sillas existentes de esta sección
                for (Seat seat : existingSeats) {
                    seatRepository.deleteById(seat.getSeatId());
                }

                // Borrar seat_blocks existentes de esta sección
                List<SeatBlock> existingBlocks = seatBlockRepository.findAllByEventSectionId(eventSectionId);
                for (SeatBlock block : existingBlocks) {
                    seatBlockRepository.deleteById(block.getSeatBlockId());
                }

                // Crear un nuevo seat_block para esta sección
                SeatBlock seatBlock = seatBlockRepository.save(SeatBlock.builder()
                        .eventSectionId(eventSectionId)
                        .name("Bloque Principal")
                        .capacity(targetSeats)
                        .build());

                // Generar las sillas
                int generatedCount = generateSeatsForBlock(
                        seatBlock.getSeatBlockId(), 
                        eventSectionId, 
                        targetSeats, 
                        rows, 
                        seatsPerRow
                );

                sectionsProcessed++;
                seatsGenerated += generatedCount;
                
                log.info("[SaveEventLayout] Sección {} regenerada: {} sillas en {} filas", 
                        eventSectionId, generatedCount, rows);
            }

            log.info("[SaveEventLayout] Regeneración completada: {} secciones procesadas, {} sillas generadas", 
                    sectionsProcessed, seatsGenerated);

        } catch (BusinessException e) {
            throw e; // Re-lanzar excepciones de negocio
        } catch (Exception e) {
            log.error("[SaveEventLayout] Error al parsear layoutData y regenerar sillas", e);
            throw new BusinessException("LAYOUT_PARSING_ERROR",
                    "Error al procesar el layout para generar sillas: " + e.getMessage());
        }
    }

    /**
     * Genera los registros de seat individuales para un seat_block.
     * 
     * Algoritmo:
     * - Distribuye targetSeats en 'rows' filas
     * - Asigna códigos legibles tipo A1, A2, B1, B2...
     * - Todas las sillas se crean con status=AVAILABLE y type=REGULAR
     */
    private int generateSeatsForBlock(Integer seatBlockId, Integer eventSectionId, 
                                      int targetSeats, int rows, int seatsPerRow) {
        int generatedCount = 0;
        int currentRow = 1;
        int currentPosition = 1;

        for (int i = 0; i < targetSeats; i++) {
            // Calcular la letra de la fila (A, B, C... Z, AA, AB...)
            String rowLabel = getRowLabel(currentRow);
            String code = rowLabel + currentPosition;

            seatRepository.save(Seat.builder()
                    .seatBlockId(seatBlockId)
                    .eventSectionId(eventSectionId)
                    .code(code)
                    .row(currentRow)
                    .position(currentPosition)
                    .type(SeatType.REGULAR)
                    .status(SeatStatus.AVAILABLE)
                    .reservedBy(null)
                    .reservedUntil(null)
                    .build());

            generatedCount++;

            // Avanzar a la siguiente posición
            currentPosition++;
            if (currentPosition > seatsPerRow) {
                currentPosition = 1;
                currentRow++;
            }
        }

        return generatedCount;
    }

    /**
     * Convierte un número de fila (1, 2, 3...) en una letra (A, B, C... Z, AA, AB...).
     * 
     * Ejemplos:
     * 1 → A
     * 2 → B
     * 26 → Z
     * 27 → AA
     * 28 → AB
     */
    private String getRowLabel(int rowNumber) {
        StringBuilder result = new StringBuilder();
        int n = rowNumber;

        while (n > 0) {
            n--; // Ajustar para índice base-0
            result.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }

        return result.toString();
    }
}
