# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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
