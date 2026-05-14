# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [develop] - 2026-05-13

### feat(storage): add image moderation via OpenNSFW2 + Sightengine cascade
#### Added
- `nsfw-service/`: FastAPI microservice wrapping OpenNSFW2 (`GET /health`, `POST /classify`). Returns `nsfw_score` (0.0–1.0) and `flagged` boolean. Rejects non-image content types and corrupted files with HTTP 400. Dockerized with `python:3.11-slim`.
- `ModerationPort` (port/out): secondary port with `isNsfwSafe(byte[])` and `isGoreSafe(byte[])`. Fail-open contract documented in Javadoc.
- `ImageModerationService` (application/service): implements `ModerationPort`, orchestrates the two-stage cascade — OpenNSFW2 first, Sightengine only if stage 1 passes. Throws `ImagePolicyViolationException` on rejection.
- `NsfwClientAdapter` (infrastructure/external): `RestTemplate` multipart POST to `${moderation.nsfw.url}/classify`. Fail-open on any infrastructure error.
- `SightengineAdapter` (infrastructure/external): `RestTemplate` multipart POST to `https://api.sightengine.com/1.0/check.json` with `models=gore`. Extracts `gore.prob`. Fail-open on any infrastructure error.
- `ImagePolicyViolationException` (shared/domain/exception): extends `BusinessException`. User-facing message is intentionally generic; internal reason code (`NSFW_CONTENT` / `VIOLENT_CONTENT`) is preserved for logging.
- `ModerationConfig` (infrastructure/config): `@ConfigurationProperties(prefix = "moderation")` with nested `Nsfw` and `Sightengine` classes. Declares `RestTemplate` bean.
- `nsfw-classifier` service in `docker-compose.yml`: internal-only (no host port), connected to `tuevento-network`, with healthcheck on `/health`. Backend `depends_on` updated.
- `moderation.*` properties added to `application-dev.yaml` (root-level, not under `app:`).
- Moderation environment variables added to `.env` and `.env.example`: `NSFW_SERVICE_URL`, `NSFW_THRESHOLD`, `SIGHTENGINE_API_USER`, `SIGHTENGINE_API_SECRET`, `SIGHTENGINE_MODELS`, `SIGHTENGINE_THRESHOLD`.

#### Modified
- `UploadFileUseCase`: injected `ImageModerationService` via `@RequiredArgsConstructor`. Moderation cascade called after `validateSize()` and before `storageClient.uploadFile()`. Skipped for non-image content types (`!contentType.startsWith("image/")`).
- `GlobalExceptionHandler`: added handler for `ImagePolicyViolationException` → HTTP 422 Unprocessable Entity.

#### Design decisions
- Cascade order (NSFW → gore) minimizes Sightengine API quota consumption (100 req/day free tier).
- Fail-open policy: moderation service failures never block legitimate uploads; errors are logged at ERROR level.
- Moderation is transparent to `StorageController` and `StorageClientPort` — zero changes to the REST interface or S3 adapter.

## [develop] - 2026-05-12

### feat(event): event module
#### Added
- Liquibase changesets 039–045: tablas `event`, `event_status_log`, `event_layout`, `event_media`, `event_media_log`, `event_rating`, `event_comment_reply` con FKs, constraints UNIQUE y CHECK via `sql` raw (compatible con Liquibase OSS)
- Domain layer: modelos puros (`Event`, `EventStatus`, `EventStatusLog`, `EventLayout`, `EventMedia`, `EventMediaLog`, `EventRating`, `EventCommentReply`), interfaces de repositorio sin dependencias de Spring, eventos de dominio inmutables (`EventCreatedEvent`, `EventStatusChangedEvent`, `EventCancelledEvent`, `EventRatingAddedEvent`, `EventMediaUploadedEvent`)
- Infrastructure layer: entidades JPA (`EventEntity` extiende `JpaAuditingEntity`, más 6 entidades sin auditoría), `JpaRepository` por entidad, mappers MapStruct, implementaciones `RepositoryImpl`; `EventMediaLogJpaRepository` incluye `@Query` para `findNextVersionByEventId`
- Application layer: 9 ports in (`CreateEventUseCase`, `UpdateEventUseCase`, `ChangeEventStatusUseCase`, `GetEventUseCase`, `DeleteEventUseCase`, `AddEventRatingUseCase`, `AddCommentReplyUseCase`, `UploadEventMediaUseCase`, `GetEventLayoutUseCase`, `SaveEventLayoutUseCase`), 9 use cases con validaciones de negocio (ownership, transiciones de estado `DRAFT→PUBLISHED→CANCELLED/COMPLETED`, unicidad, rating único por usuario, validación de layout antes de publicar)
- REST controllers: `EventController` (`/api/v1/events`), `EventRatingController` (`/api/v1/events/{eventId}/ratings`), `EventCommentController` (`/api/v1/ratings/{ratingId}/replies`), `EventMediaController` (`/api/v1/events/{eventId}/media`), `EventLayoutController` (`/api/v1/events/{eventId}/layout` — GET público + PUT `ORGANIZER`)
- `SecurityConfig` actualizado: 5 endpoints GET públicos en `PUBLIC_GET_ENDPOINTS`; `PUT /api/v1/events/*/layout` como autenticado; endpoints de escritura protegidos por rol via `@PreAuthorize`

#### Fixed
- Liquibase changeset 039: reemplazado `addCheckConstraint` (Liquibase Pro) por `sql` raw con `ALTER TABLE ... ADD CONSTRAINT ... CHECK (...)` — compatible con Liquibase OSS 4.x
- Campos `boolean isPublic` / `boolean isVisible` migrados a `Boolean` objeto en modelos de dominio, entidades JPA y DTOs de response para compatibilidad con MapStruct (Lombok genera `isXxx()` para `boolean` primitivo, que MapStruct resuelve como propiedad `xxx` en lugar de `isXxx`)
- `TestTuEventoApplication`: agregado import explícito de `TuEventoApplication` para resolver `cannot find symbol` durante `test-compile` con annotation processors activos
- `EventLayoutEntity`: agregado `@JdbcTypeCode(SqlTypes.JSON)` sobre `layoutData` para que Hibernate haga el binding correcto al tipo `jsonb` de PostgreSQL — sin esta anotación Hibernate enviaba el valor como `character varying` causando error de tipo en la columna
- `ChangeEventStatusService`: validación de layout antes de transición `DRAFT → PUBLISHED`; lanza `BusinessException("EVENT_LAYOUT_REQUIRED")` si no existe layout para el evento

## [develop] - 2026-04-24

### Theme module
#### Added
- Liquibase changeset 034: created `theme` table (id, name, description, default_palette jsonb)
- Liquibase changeset 035: created `user_theme` table with FK to `app_user` and `theme`; `is_active` flag controls the single active theme per user
- Liquibase changeset 036: created `theme_customization` table with FK to `user_theme`; stores per-property overrides as key/value pairs
- Liquibase changeset 037: created `theme_log` table with FK to `user_theme`; append-only audit log of UPDATE and RESET actions
- Liquibase changeset 038: seed data — 4 base themes with full `default_palette` JSON: `DARK` (deep purple, platform identity), `LIGHT`, `VIBRANT`, `ACCESSIBLE`
- Domain models: `Theme`, `UserTheme`, `ThemeCustomization`, `ThemeLog` — pure POJOs, no JPA, no Jackson
- Domain repository interfaces: `ThemeRepository`, `UserThemeRepository`, `ThemeCustomizationRepository`, `ThemeLogRepository` — pure interfaces, no Spring Data
- Domain events: `ThemeActivatedEvent`, `ThemeCustomizedEvent`, `ThemeCustomizationResetEvent` — immutable, primitive IDs only
- JPA entities: `ThemeEntity`, `UserThemeEntity`, `ThemeCustomizationEntity`, `ThemeLogEntity` — no `@ManyToOne`, FKs as plain `Integer`; no `JpaAuditingEntity` inheritance; `ThemeLogEntity` is append-only (no `@Setter`)
- JPA repositories: `ThemeJpaRepository`, `UserThemeJpaRepository` (`@Modifying @Query` for bulk deactivation), `ThemeCustomizationJpaRepository`, `ThemeLogJpaRepository`
- `ThemeInfraMapper`: single MapStruct mapper in `infrastructure/persistence/mapper/` handling all four entity↔domain conversions with explicit `@Mapping` for PK renames
- Domain repository implementations: `ThemeRepositoryImpl`, `UserThemeRepositoryImpl`, `ThemeCustomizationRepositoryImpl`, `ThemeLogRepositoryImpl`
- Application ports in: `GetThemesPort`, `ActivateThemePort`, `GetActivePalettePort`, `CustomizeThemePort`, `ResetCustomizationPort`, `GetThemeLogPort` — pure interfaces, no Spring
- DTOs response: `ThemeResponse` (id, name, description — no palette exposed), `ResolvedPaletteResponse` (themeId, themeName, userThemeId, `Map<String, Object>` palette), `ThemeLogResponse`
- DTO request: `CustomizeThemeRequest` (property `@NotBlank`, value `@NotBlank` + `@Pattern` for hex/rgb/rgba)
- `ThemeAppMapper`: MapStruct mapper for `Theme → ThemeResponse` and `ThemeLog → ThemeLogResponse`
- `ThemePaletteResolver`: `@Component` that parses `defaultPalette` JSON via `ObjectMapper` and overlays user customizations — decoupled from use cases
- Use cases: `GetThemesUseCase`, `ActivateThemeUseCase` (deactivates current, reactivates or creates `UserTheme`, publishes event), `GetActivePaletteUseCase` (auto-activates `DARK` theme if no active theme found), `CustomizeThemeUseCase` (upsert customization, audit log, event), `ResetCustomizationUseCase` (delete customization, audit log with `oldValue`, event), `GetThemeLogUseCase`
- `ThemeController`: base path `/api/v1/themes`, 6 endpoints (see below)
- `SecurityConfig` updated: `GET /api/v1/themes` added to public routes via `PUBLIC_GET_ENDPOINTS`; authenticated routes added as explicit `requestMatchers` by HTTP method

#### Endpoints
- `GET /api/v1/themes` — public; returns list of available themes
- `POST /api/v1/themes/activate/{themeId}` — authenticated; activates theme for current user, returns resolved palette
- `GET /api/v1/themes/my-active` — authenticated; returns current user's resolved palette (activates DARK by default if none set)
- `PUT /api/v1/themes/my-active/customize` — authenticated; overrides a palette property, returns updated resolved palette
- `DELETE /api/v1/themes/my-active/customize/{property}` — authenticated; resets a property to theme default, returns updated resolved palette
- `GET /api/v1/themes/my-active/log` — authenticated; returns audit log of theme changes for current user

#### Design decisions
- Resolved palette strategy: backend merges `default_palette` + user customizations and returns a ready-to-consume `Map<String, Object>` — both web (React + Tailwind v4) and mobile (React Native + NativeWind) share the same single source of truth
- `DARK` is the default theme (deep purple identity palette matching the existing frontend); auto-activated on first `GET /my-active` if the user has no active theme
- `ThemePaletteResolver` kept as a separate `@Component` to avoid coupling merge logic to individual use cases

## [develop] - 2026-04-07

### Security module
#### Fixed
- Fixed: activation code not validated against request email (security vulnerability)
- Fixed: recovery code not validated against request email (security vulnerability)
- Fixed: logout not validating session ownership (security vulnerability)
- Fixed: login not checking BLOCKED/INACTIVE/DELETED user status
- Fixed: lockout message exposed exact timestamp
- Fixed: recover password revealed email existence (user enumeration)
- Reverted: recover password now returns explicit error when email is not registered
- Fixed: refresh token not validating user active status
- Fixed: email send failure in register now logged instead of propagated
- Fixed: OAuth login allows duplicate email with local account
- Fixed: organizer request allowed for existing organizers
#### Added
- Added: AccessDeniedException handler (403)
- Added: input validations on register: Gmail-only email, strong password, full name format
- Added: password strength validation on change password to match register rules- Added: AuthenticationException handler (401)
- Added: MethodNotAllowedException handler (405)
- Added: ConstraintViolationException handler (409)
- Added: NoResourceFoundException handler (404)

### Profile module
#### Added
- Liquibase changeset 031: inserted `PROFILE_PICTURE` file category (public, jpg/jpeg/png/webp, max 2MB)
- Liquibase changeset 032: created `profile` table with FK to `app_user`, `city` (nullable) and `stored_file` (nullable)
- Liquibase changeset 033: created `profile_log` table with FK to `profile`
- Domain models: `Profile` (references `City` object from geolocation module), `ProfileLog`
- Domain repository interfaces: `ProfileRepository`, `ProfileLogRepository`
- Domain events: `ProfileCreatedEvent`, `ProfileUpdatedEvent` (primitive IDs only)
- Domain exception: `ProfileAlreadyExistsException`
- JPA entities: `ProfileEntity` (auditable, `storedFileId` as plain INT decoupled from storage module), `ProfileLogEntity`
- MapStruct mappers: `ProfileMapper` (uses `CityMapper`), `ProfileLogMapper`
- JPA repositories: `ProfileJpaRepository`, `ProfileLogJpaRepository`
- Domain repository implementations: `ProfileRepositoryImpl`, `ProfileLogRepositoryImpl`
- Application ports in: `CreateProfilePort`, `UpdateProfilePort`, `GetProfilePort`, `GetProfileByUserIdPort`
- DTOs: `CreateProfileRequest` (userId, fullName), `UpdateProfileRequest` (cityId, storedFileId, fullName, bio), `ProfileResponse`
- Use cases: `CreateProfileUseCase` (validates no duplicate profile per user, assigns default avatar), `UpdateProfileUseCase` (logs each changed field to profile_log), `GetProfileUseCase`, `GetProfileByUserIdUseCase`
- `ProfileController`: POST `/api/v1/profiles`, PUT `/api/v1/profiles/{profileId}`, GET `/api/v1/profiles/{profileId}`, GET `/api/v1/profiles/user/{userId}`
- `ProfileDataInitializer`: uploads `default-avatar.jpg` from `src/main/resources/assets/` to MinIO on first startup using `UploadFilePort`; logs resulting `storedFileId` for manual config
- `app.profile.default-avatar-stored-file-id` property added to `application-dev.yaml`
- GET profile endpoints added to public routes in `SecurityConfig`
- `src/main/resources/assets/default-avatar.jpg` placeholder directory created

#### Known issues
- `ProfileDataInitializer` lookup for existing default avatar pending fix (method to check `findByOwnerEntity` not yet validated)

## [develop] - 2026-03-29

### Storage module
#### Added
- MinIO service added to docker-compose for local S3-compatible storage
- AWS S3 SDK dependency added to pom.xml
- Liquibase changesets 018-022: storage_provider, file_category, stored_file, storage_operation_log tables and initial MinIO provider
- S3StorageClient: AWS S3 SDK implementation of StorageClientPort compatible with MinIO
- S3Config: S3Client and S3Presigner beans configured for MinIO in development
- StorageController: REST endpoints for file upload, delete, get and url generation
- Initial file category: ORGANIZER_DOCUMENT (PDF, max 5MB, private)
- organizer_petition migrated from BYTEA document to stored_file_id FK referencing storage module
- CreateProfileRequest: added storedFileIdSet flag to distinguish explicit null from use-default behavior

### Geolocation module#### Added
- Liquibase changesets 025-027: department, city and site tables
- GeolocationController: REST endpoints for departments, cities and sites
- Seed data: 33 departments and 1122 cities of Colombia via Liquibase loadData (changesets 028-029)

### Backend

### Security module — completed and merged from feature/security-module
#### Added
- Liquibase changesets for all security tables: user_status, role, permission, app_user, role_permission, login_credentials, account_activation, account_lockout, auth_session, refresh_token, recover_password, password_history, oauth_account, user_status_history, organizer_petition
- Audit columns (created_at, updated_at, created_by, updated_by) to all auditable tables
- Domain models: User, Role, Permission, UserStatus, UserStatusHistory, AuthSession, RefreshToken, LoginCredentials, AccountActivation, AccountLockout, RecoverPassword, PasswordHistory, OauthAccount, OrganizerPetition
- Domain repository interfaces for all models
- Domain events: UserRegisteredEvent, UserActivatedEvent, UserLockedEvent, PasswordChangedEvent, OrganizerPetitionCreatedEvent
- JPA entities and MapStruct mappers for all models
- JPA repository implementations
- Domain repository implementations
- Application ports (in/out) for all use cases
- Request/Response DTOs with Jakarta validation
- Use cases: RegisterUser, ActivateAccount, Login, Logout, RefreshToken, RecoverPassword, ResetPassword, ChangePassword, LinkOauthAccount, OauthLogin, RequestOrganizer
- AliasGenerator utility for automatic alias generation from email
- OauthProfile DTO for OAuth provider profile mapping
- External port implementations: JwtTokenGenerator (JJWT), BcryptPasswordEncoder, JavaMailEmailNotification, SecureRandomCodeGenerator
- OAuth Google configuration with extensible provider pattern
- Spring Security configuration with JWT authentication filter
- REST controllers: AuthController, UserController
- Initial data: roles (ADMIN, ORGANIZER, USER) and user statuses (PENDING, ACTIVE, BLOCKED, INACTIVE, DELETED) via Liquibase changeset
- DataInitializer: default admin user created on startup from environment variables
- SecurityDataCleanupTask: scheduled task running daily at 2am to clean expired/revoked auth sessions, refresh tokens, activation codes and recovery codes
- Swagger JWT Bearer Authentication scheme configured for protected endpoints
- Admin endpoints for organizer petition management: list pending requests, approve and reject
- OrganizerApprovedEvent domain event

#### Fixed
- Duplicate SecurityConfig bean conflict resolved
- Missing audit columns added to auditable tables via changeset 016
- BYTEA mapping fixed in OrganizerPetitionEntity replacing @Lob with @Column(columnDefinition = "bytea")
- RegisterUserUseCase: default role code corrected from "ATTENDEE" to "USER"
- OauthLoginUseCase: default role code corrected from "ATTENDEE" to "USER"
- ActivateAccountUseCase: user status now updated to "ACTIVE" after successful account activation
- ChangePasswordUseCase: fixed user lookup using SecurityUser.getUserId() instead of Authentication.getName() which was returning alias instead of email
- SecurityUser: fixed authority registration removing ROLE_ prefix to match hasAuthority("ADMIN") checks in SecurityConfig and AdminController
- JwtAuthenticationFilter: replaced fragile manual Base64 claim parser with JJWT-based extraction via TokenGeneratorPort; added debug log for loaded authorities
- SecurityConfig: added @EnableMethodSecurity to enable @PreAuthorize on controllers
- TokenGeneratorPort / JwtTokenGenerator: added extractRole() and extractSubject() methods
