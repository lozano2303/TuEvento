# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Backend

### Security module
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

#### Fixed
- Duplicate SecurityConfig bean conflict resolved
- Missing audit columns added to auditable tables via changeset 016
- BYTEA mapping fixed in OrganizerPetitionEntity replacing @Lob with @Column(columnDefinition = "bytea")
