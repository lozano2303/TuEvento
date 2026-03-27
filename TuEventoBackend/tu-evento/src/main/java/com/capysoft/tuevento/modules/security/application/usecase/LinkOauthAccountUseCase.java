package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.LinkOauthAccountRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LinkOauthAccountResponse;
import com.capysoft.tuevento.modules.security.application.port.in.LinkOauthAccountPort;
import com.capysoft.tuevento.modules.security.domain.model.OauthAccount;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.OauthAccountRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LinkOauthAccountUseCase implements LinkOauthAccountPort {

    private final OauthAccountRepository oauthAccountRepository;
    private final UserRepository         userRepository;

    @Override
    @Transactional
    public LinkOauthAccountResponse link(LinkOauthAccountRequest request) {
        String alias = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByAlias(alias)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        oauthAccountRepository.findByUserIdAndProvider(user.getUserId(), request.getProvider())
                .ifPresent(existing -> {
                    throw new BusinessException("OAUTH_ALREADY_LINKED",
                            "Provider " + request.getProvider() + " is already linked to this account");
                });

        oauthAccountRepository.findByProviderAndProviderUserId(
                request.getProvider(), request.getProviderUserId())
                .ifPresent(existing -> {
                    throw new BusinessException("OAUTH_PROVIDER_TAKEN",
                            "This provider account is already linked to another user");
                });

        LocalDateTime now = LocalDateTime.now();
        OauthAccount saved = oauthAccountRepository.save(OauthAccount.builder()
                .user(user)
                .provider(request.getProvider())
                .providerUserId(request.getProviderUserId())
                .email(request.getEmail())
                .linkedAt(now)
                .build());

        return LinkOauthAccountResponse.builder()
                .oauthAccountId(saved.getOauthAccountId())
                .provider(saved.getProvider())
                .email(saved.getEmail())
                .linkedAt(saved.getLinkedAt())
                .build();
    }
}
