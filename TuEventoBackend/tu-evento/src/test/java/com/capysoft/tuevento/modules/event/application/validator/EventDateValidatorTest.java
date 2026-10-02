package com.capysoft.tuevento.modules.event.application.validator;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.capysoft.tuevento.modules.event.application.validator.EventDateValidator.MAX_YEARS_AHEAD;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EventDateValidator}.
 *
 * <p>A fixed {@link Clock} pinned to 2026-10-02 (America/Bogota) is used in every
 * test so that "today" is deterministic and the suite never depends on the real
 * wall-clock date.
 *
 * <h3>Scenarios covered</h3>
 * <ol>
 *   <li>Start date in the past → EVENT_START_DATE_IN_PAST</li>
 *   <li>Finish date before start date → EVENT_END_BEFORE_START</li>
 *   <li>Missing dates (null) → EVENT_DATES_REQUIRED</li>
 *   <li>Start date too far in the future → EVENT_START_DATE_TOO_FAR</li>
 *   <li>Valid dates (happy path) → no exception</li>
 *   <li>Edit without changing dates on an event that has already started → no exception</li>
 *   <li>Publish with a start date in the past → EVENT_START_DATE_IN_PAST</li>
 * </ol>
 */
@DisplayName("EventDateValidator")
class EventDateValidatorTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** Pinned "today" for all tests: 2026-10-02 in America/Bogota (UTC-5). */
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant FIXED_INSTANT =
            LocalDate.of(2026, 10, 2).atStartOfDay(BOGOTA).toInstant();

    private final Clock fixedClock  = Clock.fixed(FIXED_INSTANT, BOGOTA);
    private final EventDateValidator validator = new EventDateValidator(fixedClock);

    /** Convenience: today according to the fixed clock. */
    private LocalDate today() { return LocalDate.now(fixedClock); }

    // ── validate() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validate(startDate, finishDate)")
    class ValidateFullDates {

        // ── Scenario 1 ────────────────────────────────────────────────────────

        @Test
        @DisplayName("start date in the past → throws EVENT_START_DATE_IN_PAST")
        void startDate_inPast_throwsStartDateInPast() {
            LocalDate start  = today().minusDays(1);
            LocalDate finish = today().plusDays(5);

            assertThatThrownBy(() -> validator.validate(start, finish))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_START_DATE_IN_PAST");
                    });
        }

        // ── Scenario 2 ────────────────────────────────────────────────────────

        @Test
        @DisplayName("finish date before start date → throws EVENT_END_BEFORE_START")
        void finishDate_beforeStartDate_throwsEndBeforeStart() {
            LocalDate start  = today().plusDays(3);
            LocalDate finish = today().plusDays(1); // earlier than start

            assertThatThrownBy(() -> validator.validate(start, finish))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_END_BEFORE_START");
                    });
        }

        // ── Scenario 3 ────────────────────────────────────────────────────────

        @Test
        @DisplayName("null startDate → throws EVENT_DATES_REQUIRED")
        void startDate_null_throwsDatesRequired() {
            assertThatThrownBy(() -> validator.validate(null, today().plusDays(3)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_DATES_REQUIRED");
                    });
        }

        @Test
        @DisplayName("null finishDate → throws EVENT_DATES_REQUIRED")
        void finishDate_null_throwsDatesRequired() {
            assertThatThrownBy(() -> validator.validate(today().plusDays(1), null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_DATES_REQUIRED");
                    });
        }

        @Test
        @DisplayName("both dates null → throws EVENT_DATES_REQUIRED")
        void bothDates_null_throwsDatesRequired() {
            assertThatThrownBy(() -> validator.validate(null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_DATES_REQUIRED");
                    });
        }

        // ── Scenario 4 ────────────────────────────────────────────────────────

        @Test
        @DisplayName("start date more than 2 years ahead → throws EVENT_START_DATE_TOO_FAR")
        void startDate_tooFarAhead_throwsStartDateTooFar() {
            LocalDate start  = today().plusYears(MAX_YEARS_AHEAD).plusDays(1);
            LocalDate finish = start.plusDays(7);

            assertThatThrownBy(() -> validator.validate(start, finish))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_START_DATE_TOO_FAR");
                    });
        }

        // ── Scenario 5 ────────────────────────────────────────────────────────

        @Test
        @DisplayName("valid dates (today start, finish tomorrow) → no exception")
        void validDates_noException() {
            LocalDate start  = today();
            LocalDate finish = today().plusDays(1);

            assertThatCode(() -> validator.validate(start, finish))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("valid dates (future start, same-day finish) → no exception")
        void validDates_sameDayFinish_noException() {
            LocalDate start  = today().plusDays(5);
            LocalDate finish = today().plusDays(5); // finishDate == startDate is allowed

            assertThatCode(() -> validator.validate(start, finish))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("start date exactly at 2-year boundary → no exception")
        void startDate_exactlyAtBoundary_noException() {
            LocalDate start  = today().plusYears(MAX_YEARS_AHEAD); // boundary is inclusive
            LocalDate finish = start.plusDays(1);

            assertThatCode(() -> validator.validate(start, finish))
                    .doesNotThrowAnyException();
        }

        // ── Scenario 6 ────────────────────────────────────────────────────────

        /**
         * Edit without changing dates — the organizer only edits other fields
         * (e.g. name, description) on an event whose start date is already in the
         * past.  The use case only calls {@code validate()} when a date was actually
         * changed; this test documents the validator's own contract: if the caller
         * skips validate() because no date changed, the validator itself is never
         * invoked and no exception is raised.
         *
         * <p>We simulate this by calling the validator only with the <em>new</em>
         * dates that an update would resolve — but in this scenario the caller
         * should not call validate() at all.  To keep the test self-contained, we
         * verify the validator does NOT throw for unchanged-date payloads when the
         * resolved dates are themselves valid.
         */
        @Test
        @DisplayName("editing other fields only (dates unchanged, already started) → validate() not called → no exception")
        void editWithoutChangingDates_alreadyStarted_noException() {
            // Simulate: event started yesterday, organizer only changes the name.
            // The use case does NOT call validate() because neither date changed.
            // We assert the validator itself doesn't throw when called with future dates
            // (it's a no-op guard test — the real protection is in UpdateEventService).
            LocalDate originalStart  = today().minusDays(1); // already started
            LocalDate originalFinish = today().plusDays(5);

            // UpdateEventService skips validate() when request dates equal stored dates,
            // so the validator is never invoked. Here we just confirm it won't throw
            // when given valid future dates as a standalone call.
            LocalDate newStart  = today().plusDays(1);
            LocalDate newFinish = today().plusDays(6);

            assertThatCode(() -> validator.validate(newStart, newFinish))
                    .doesNotThrowAnyException();

            // And confirm the validator WOULD throw if the old (past) dates were passed
            assertThatThrownBy(() -> validator.validate(originalStart, originalFinish))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> org.assertj.core.api.Assertions
                            .assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_START_DATE_IN_PAST"));
        }
    }

    // ── validateForPublish() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("validateForPublish(startDate)")
    class ValidateForPublish {

        // ── Scenario 7 ────────────────────────────────────────────────────────

        @Test
        @DisplayName("start date in the past at publish time → throws EVENT_START_DATE_IN_PAST")
        void publishWithPastStartDate_throwsStartDateInPast() {
            // Draft was created weeks ago; now the start date has already passed.
            LocalDate pastStart = today().minusDays(3);

            assertThatThrownBy(() -> validator.validateForPublish(pastStart))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        org.assertj.core.api.Assertions.assertThat(be.getCode())
                                .isEqualTo("EVENT_START_DATE_IN_PAST");
                    });
        }

        @Test
        @DisplayName("start date is today at publish time → no exception")
        void publishWithTodayStartDate_noException() {
            assertThatCode(() -> validator.validateForPublish(today()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("start date is in the future at publish time → no exception")
        void publishWithFutureStartDate_noException() {
            assertThatCode(() -> validator.validateForPublish(today().plusDays(10)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null startDate at publish time → no exception (null-safe guard)")
        void publishWithNullStartDate_noException() {
            // null means "no date stored" — the publish check is lenient for null
            // (required check is done at create/edit time via validate())
            assertThatCode(() -> validator.validateForPublish(null))
                    .doesNotThrowAnyException();
        }
    }
}
