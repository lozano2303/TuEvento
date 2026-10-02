package com.capysoft.tuevento.modules.event.application.validator;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Shared date-validation logic for event creation, editing, and publishing.
 *
 * <p>A single class centralises all four rules so that
 * {@code CreateEventService}, {@code UpdateEventService},
 * {@code ChangeEventStatusService}, and {@code AdminChangeEventStatusUseCase}
 * never duplicate this logic.
 *
 * <h3>Rules</h3>
 * <ol>
 *   <li>Both {@code startDate} and {@code finishDate} must be present
 *       → {@code EVENT_DATES_REQUIRED}</li>
 *   <li>{@code startDate} must be today or in the future
 *       → {@code EVENT_START_DATE_IN_PAST}</li>
 *   <li>{@code startDate} must not be more than 2 years from today
 *       → {@code EVENT_START_DATE_TOO_FAR}</li>
 *   <li>{@code finishDate} must be on or after {@code startDate}
 *       → {@code EVENT_END_BEFORE_START}</li>
 * </ol>
 *
 * <h3>Publish-only check</h3>
 * {@link #validateForPublish(LocalDate)} re-runs rule 2 alone so that a draft
 * created weeks ago is caught at publish time even if it passed creation checks.
 */
@Component
@RequiredArgsConstructor
public class EventDateValidator {

    /** Maximum allowed gap between today and startDate (inclusive boundary). */
    static final int MAX_YEARS_AHEAD = 2;

    private final Clock clock;

    // ── Full validation (create / edit) ──────────────────────────────────────

    /**
     * Validates both dates for create and edit operations.
     *
     * @param startDate  the proposed event start date (must not be null)
     * @param finishDate the proposed event finish date (must not be null)
     * @throws BusinessException with the appropriate error code on the first violation found
     */
    public void validate(LocalDate startDate, LocalDate finishDate) {
        if (startDate == null || finishDate == null) {
            throw new BusinessException("EVENT_DATES_REQUIRED",
                    "Both startDate and finishDate are required");
        }

        LocalDate today = LocalDate.now(clock);

        if (startDate.isBefore(today)) {
            throw new BusinessException("EVENT_START_DATE_IN_PAST",
                    "startDate cannot be in the past");
        }

        if (startDate.isAfter(today.plusYears(MAX_YEARS_AHEAD))) {
            throw new BusinessException("EVENT_START_DATE_TOO_FAR",
                    "startDate cannot be more than " + MAX_YEARS_AHEAD + " years from today");
        }

        if (finishDate.isBefore(startDate)) {
            throw new BusinessException("EVENT_END_BEFORE_START",
                    "finishDate must be on or after startDate");
        }
    }

    // ── Publish-only check ────────────────────────────────────────────────────

    /**
     * Re-validates the start date at publish time.
     *
     * <p>A draft may have been created weeks before publishing; this check ensures
     * the event's start date is still in the future (or today) when it goes live.
     *
     * @param startDate the stored start date of the event being published
     * @throws BusinessException with code {@code EVENT_START_DATE_IN_PAST} if the
     *                           start date has already passed
     */
    public void validateForPublish(LocalDate startDate) {
        LocalDate today = LocalDate.now(clock);
        if (startDate != null && startDate.isBefore(today)) {
            throw new BusinessException("EVENT_START_DATE_IN_PAST",
                    "Cannot publish: startDate is in the past");
        }
    }
}
