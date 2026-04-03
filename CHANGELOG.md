# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [develop] - 2026-03-29

### Storage module
#### Added
- MinIO service added to docker-compose for local S3-compatible storage
- AWS S3 SDK dependency added to pom.xml
- Liquibase changesets 018-022: storage_provider, file_category, stored_file, storage_operation_log tables and initial MinIO provider
- S3StorageClient: AWS S3 SDK implementation of StorageClientPort compatible with MinIO
- S3Config: S3Client and S3Presigner beans configured for MinIO in development

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
