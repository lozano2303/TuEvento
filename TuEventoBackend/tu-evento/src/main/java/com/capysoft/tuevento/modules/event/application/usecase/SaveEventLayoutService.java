package com.capysoft.tuevento.modules.event.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveEventLayoutService implements SaveEventLayoutUseCase {

    /**
     * Máximo de sillas por sección lógica. El layout llega del cliente: sin tope, un
     * targetSeats enorme reservaría memoria sin límite. 200.000 cubre con margen el
     * estadio más grande del mundo (~132.000 asientos) como una sola sección.
     */
    static final int MAX_SEATS_PER_SECTION = 200_000;

    /** Estados que impiden regenerar una sección: alguien ya reservó o compró. Las cortesías sí se regeneran. */
    private static final Set<SeatStatus> TAKEN_STATUSES = EnumSet.of(SeatStatus.RESERVED, SeatStatus.SOLD);

    private final EventRepository eventRepository;
    private final EventLayoutRepository eventLayoutRepository;
    private final SeatBlockRepository seatBlockRepository;
    private final SeatRepository seatRepository;
    private final EventSectionRepository eventSectionRepository;
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
        // El layout anterior permite saltar las secciones cuyas sillas no cambiaron
        String previousLayoutData = existing.map(EventLayout::getLayoutData).orElse(null);

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
        regenerateSeatsFromLayout(eventId, saved.getLayoutData(), previousLayoutData);

        return EventLayoutResponse.builder()
                .eventLayoutId(saved.getEventLayoutId())
                .eventId(saved.getEventId())
                .layoutData(saved.getLayoutData())
                .build();
    }

    /**
     * Parsea el layoutData JSON y regenera los registros de seat_block y seat
     * para cada sección lógica (agrupando elementos por backendSectionId).
     * 
     * Estrategia ACTUALIZADA: agrupa elementos por backendSectionId y trata
     * cada grupo como una sección lógica única con numeración continua de sillas.
     * Esto soporta sub-secciones visuales (ej: "General 1", "General 2") que
     * comparten el mismo tipo y precio pero tienen numeración de sillas continua.
     * 
     * Corre en la misma transacción que guarda el EventLayout — si algo falla,
     * se revierten ambos cambios.
     */
    private void regenerateSeatsFromLayout(Long eventId, String layoutData, String previousLayoutData) {
        try {
            JsonNode root = objectMapper.readTree(layoutData);
            JsonNode elementsNode = root.get("elements");

            if (elementsNode == null || !elementsNode.isArray()) {
                log.warn("[SaveEventLayout] layoutData no contiene 'elements' array válido");
                return;
            }

            // Los ids de sección vienen del cliente: solo se aceptan las secciones de este evento,
            // para que un layout no pueda borrar ni regenerar sillas de otro evento
            Map<Integer, EventSection> eventSections = eventSectionRepository.findAllByEventId(Math.toIntExact(eventId)).stream()
                    .collect(Collectors.toMap(EventSection::getEventSectionId, Function.identity()));

            // Paso 1: Agrupar elementos de sección por backendSectionId
            Map<Integer, List<SectionElement>> sectionGroups = groupSectionElements(elementsNode, (sectionId, element) -> {
                if (!eventSections.containsKey(sectionId)) {
                    throw new BusinessException("LAYOUT_SECTION_NOT_IN_EVENT",
                            "La sección " + sectionId + " no pertenece a este evento");
                }
                if (element.targetSeats > MAX_SEATS_PER_SECTION) {
                    throw new BusinessException("LAYOUT_TOO_MANY_SEATS",
                            "Una sección no puede tener más de " + MAX_SEATS_PER_SECTION + " sillas");
                }
            });

            // Validar el total y las cortesías de cada sección lógica antes de tocar ninguna
            // (long: la suma de sub-elementos no puede desbordar)
            for (List<SectionElement> elements : sectionGroups.values()) {
                long totalSeats = elements.stream().mapToLong(e -> e.targetSeats).sum();
                if (totalSeats > MAX_SEATS_PER_SECTION) {
                    throw new BusinessException("LAYOUT_TOO_MANY_SEATS",
                            "Una sección no puede tener más de " + MAX_SEATS_PER_SECTION + " sillas");
                }
                int courtesySeats = courtesySeatsOf(elements);
                if (courtesySeats < 0 || courtesySeats > totalSeats) {
                    throw new BusinessException("LAYOUT_INVALID_COURTESY_SEATS",
                            "Las sillas de cortesía de una sección deben estar entre 0 y sus " + totalSeats + " sillas");
                }
            }

            Map<Integer, List<Integer>> previousSignatures = seatSignatures(previousLayoutData);
            int sectionsSkipped = 0;

            // Paso 2: Procesar cada sección lógica (grupo)
            int sectionsProcessed = 0;
            int seatsGenerated = 0;

            for (Map.Entry<Integer, List<SectionElement>> entry : sectionGroups.entrySet()) {
                Integer eventSectionId = entry.getKey();
                List<SectionElement> elements = entry.getValue();
                int totalCapacity = elements.stream().mapToInt(e -> e.targetSeats).sum();

                // Sin cambios en sus sillas y con todas ya generadas: no se borra ni se inserta nada.
                // Guardar un layout de 55.000 sillas donde solo se movió algo ya no las regenera todas
                if (signature(elements).equals(previousSignatures.get(eventSectionId))
                        && seatRepository.countByEventSectionId(eventSectionId) == totalCapacity) {
                    sectionsSkipped++;
                    continue;
                }

                // Validación defensiva: verificar que ninguna silla existente esté reservada/vendida
                // Una sola consulta (EXISTS) en lugar de cargar todas las sillas en memoria
                if (seatRepository.existsByEventSectionIdAndStatusIn(eventSectionId, TAKEN_STATUSES)) {
                    throw new BusinessException("SEAT_REGENERATION_CONFLICT",
                            "No se puede regenerar las sillas de la sección " + eventSectionId +
                            " porque algunas ya tienen reservas o ventas. " +
                            "Esto no debería ocurrir en estado DRAFT.");
                }

                // Borrar sillas y seat_blocks existentes de esta sección: una sentencia por tabla
                seatRepository.deleteAllByEventSectionId(eventSectionId);
                seatBlockRepository.deleteAllByEventSectionId(eventSectionId);

                // Crear un único seat_block para toda la sección lógica
                SeatBlock seatBlock = seatBlockRepository.save(SeatBlock.builder()
                        .eventSectionId(eventSectionId)
                        .name("Bloque Principal")
                        .capacity(totalCapacity)
                        .build());

                // Generar sillas con numeración continua a través de todos los elementos
                // Las sillas se construyen en memoria y se insertan por lotes
                int courtesySeats = courtesySeatsOf(elements);
                List<Seat> seats = buildContinuousSeatsForSection(
                        seatBlock.getSeatBlockId(),
                        eventSectionId,
                        elements,
                        courtesySeats
                );
                seatRepository.insertAll(seats);
                int generatedCount = seats.size();

                // La sección vende todas sus sillas menos las cortesías
                EventSection section = eventSections.get(eventSectionId);
                eventSectionRepository.save(EventSection.builder()
                        .eventSectionId(section.getEventSectionId())
                        .eventId(section.getEventId())
                        .sectionTypeId(section.getSectionTypeId())
                        .capacity(totalCapacity)
                        .availableSeats(totalCapacity - courtesySeats)
                        .price(section.getPrice())
                        .isActive(section.getIsActive())
                        .build());

                sectionsProcessed++;
                seatsGenerated += generatedCount;
                
                log.info("[SaveEventLayout] Sección {} regenerada: {} sillas continuas en {} sub-elementos", 
                        eventSectionId, generatedCount, elements.size());
            }

            log.info("[SaveEventLayout] Regeneración completada: {} secciones regeneradas ({} sillas), {} sin cambios",
                    sectionsProcessed, seatsGenerated, sectionsSkipped);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SaveEventLayout] Error al parsear layoutData y regenerar sillas", e);
            throw new BusinessException("LAYOUT_PARSING_ERROR",
                    "Error al procesar el layout para generar sillas: " + e.getMessage());
        }
    }

    /**
     * Clase auxiliar para representar un elemento de sección con su configuración de asientos.
     */
    static class SectionElement {
        public final int targetSeats;
        public final int rows;
        public final int seatsPerRow;
        public final int courtesySeats; // Cortesías de toda la sección lógica (el editor las repite en cada parte)
        public final int originalIndex; // Para mantener orden determinístico

        SectionElement(int targetSeats, int rows, int seatsPerRow, int courtesySeats, int originalIndex) {
            this.targetSeats = targetSeats;
            this.rows = rows;
            this.seatsPerRow = seatsPerRow;
            this.courtesySeats = courtesySeats;
            this.originalIndex = originalIndex;
        }
    }

    /** Regla que se aplica a cada elemento de sección al agruparlo (validaciones del layout nuevo). */
    @FunctionalInterface
    interface SectionElementCheck {
        void check(Integer sectionId, SectionElement element);
    }

    /**
     * Agrupa los elementos de sección con sillas por backendSectionId, cada grupo ordenado
     * por su posición en el layout. Ignora los elementos sin sección, sin seatLayout o sin sillas.
     */
    private static Map<Integer, List<SectionElement>> groupSectionElements(JsonNode elementsNode,
                                                                          SectionElementCheck check) {
        Map<Integer, List<SectionElement>> groups = new HashMap<>();
        for (int i = 0; i < elementsNode.size(); i++) {
            JsonNode elementNode = elementsNode.get(i);
            JsonNode backendSectionIdNode = elementNode.get("backendSectionId");
            JsonNode seatLayoutNode = elementNode.get("seatLayout");
            if (!"section".equals(elementNode.path("type").asText(null))
                    || backendSectionIdNode == null || backendSectionIdNode.isNull()
                    || seatLayoutNode == null || seatLayoutNode.isNull()) {
                continue;
            }
            SectionElement element = new SectionElement(
                    seatLayoutNode.path("targetSeats").asInt(0),
                    seatLayoutNode.path("rows").asInt(1),
                    seatLayoutNode.path("seatsPerRow").asInt(1),
                    elementNode.path("courtesySeats").asInt(0),
                    i);
            if (element.targetSeats <= 0) {
                continue;
            }
            Integer sectionId = backendSectionIdNode.asInt();
            check.check(sectionId, element);
            groups.computeIfAbsent(sectionId, k -> new ArrayList<>()).add(element);
        }
        groups.values().forEach(elements -> elements.sort(Comparator.comparingInt(e -> e.originalIndex)));
        return groups;
    }

    /**
     * Firma de las sillas de cada sección en un layout: lo que determina cuántas sillas se
     * generan, su numeración y sus cortesías. Un layout ilegible no tiene firmas: se regenera todo.
     */
    private Map<Integer, List<Integer>> seatSignatures(String layoutData) {
        if (layoutData == null) {
            return Map.of();
        }
        try {
            JsonNode elementsNode = objectMapper.readTree(layoutData).get("elements");
            if (elementsNode == null || !elementsNode.isArray()) {
                return Map.of();
            }
            Map<Integer, List<Integer>> signatures = new HashMap<>();
            groupSectionElements(elementsNode, (sectionId, element) -> { })
                    .forEach((sectionId, elements) -> signatures.put(sectionId, signature(elements)));
            return signatures;
        } catch (JsonProcessingException e) {
            log.warn("[SaveEventLayout] No se pudo leer el layout anterior; se regeneran todas las secciones");
            return Map.of();
        }
    }

    private static List<Integer> signature(List<SectionElement> elements) {
        List<Integer> signature = new ArrayList<>(elements.size() * 2 + 1);
        signature.add(courtesySeatsOf(elements));
        for (SectionElement element : elements) {
            signature.add(element.targetSeats);
            signature.add(element.seatsPerRow);
        }
        return signature;
    }

    /** Cortesías de una sección lógica: las del primer elemento (ya ordenados por índice). */
    private static int courtesySeatsOf(List<SectionElement> elements) {
        return elements.get(0).courtesySeats;
    }

    /**
     * Construye las sillas con numeración continua para una sección lógica compuesta
     * por múltiples elementos visuales (sub-secciones). No las guarda: el llamador las
     * inserta por lotes.
     *
     * La numeración de códigos (A1, A2, B1...) continúa a través de todos los
     * elementos del grupo, sin reiniciarse entre sub-secciones.
     *
     * Las primeras {@code courtesySeats} sillas (A1, A2…) quedan como cortesía (tipo y estado
     * COURTESY): no se pueden reservar ni comprar.
     */
    List<Seat> buildContinuousSeatsForSection(Integer seatBlockId, Integer eventSectionId,
                                              List<SectionElement> elements, int courtesySeats) {
        int total = elements.stream().mapToInt(e -> e.targetSeats).sum();
        List<Seat> seats = new ArrayList<>(total);
        int globalCurrentRow = 1;
        int globalCurrentPosition = 1;

        // Calcular seatsPerRow máximo para mantener consistencia en la numeración
        int maxSeatsPerRow = elements.stream().mapToInt(e -> e.seatsPerRow).max().orElse(1);

        for (SectionElement element : elements) {
            for (int i = 0; i < element.targetSeats; i++) {
                // Usar numeración global continua
                String rowLabel = getRowLabel(globalCurrentRow);
                String code = rowLabel + globalCurrentPosition;
                boolean courtesy = seats.size() < courtesySeats;

                seats.add(Seat.builder()
                        .seatBlockId(seatBlockId)
                        .eventSectionId(eventSectionId)
                        .code(code)
                        .row(globalCurrentRow)
                        .position(globalCurrentPosition)
                        .type(courtesy ? SeatType.COURTESY : SeatType.REGULAR)
                        .status(courtesy ? SeatStatus.COURTESY : SeatStatus.AVAILABLE)
                        .reservedBy(null)
                        .reservedUntil(null)
                        .build());

                // Avanzar numeración global usando el seatsPerRow máximo para consistencia
                globalCurrentPosition++;
                if (globalCurrentPosition > maxSeatsPerRow) {
                    globalCurrentPosition = 1;
                    globalCurrentRow++;
                }
            }
        }

        return seats;
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
