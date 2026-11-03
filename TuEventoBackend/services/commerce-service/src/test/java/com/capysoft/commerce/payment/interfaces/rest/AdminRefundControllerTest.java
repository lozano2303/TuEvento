package com.capysoft.commerce.payment.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.commerce.wallet.application.usecase.BulkCreditWalletUseCaseImpl;
import com.capysoft.commerce.wallet.interfaces.rest.AdminWalletController;

/** Endpoints de admin de Comercio: misma URL que cuando estaban en el AdminController de Identidad. */
@SpringJUnitConfig({AdminRefundController.class, AdminWalletController.class,
        AdminRefundControllerTest.MethodSecurityConfig.class})
@DisplayName("Admin de Comercio (reembolsos y crédito masivo)")
class AdminRefundControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @MockitoBean GetCommerceAdminStatsPort getCommerceAdminStatsPort;
    @MockitoBean BulkCreditWalletUseCaseImpl bulkCreditWalletUseCase;

    @Autowired AdminRefundController refunds;
    @Autowired AdminWalletController wallet;

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("el tamaño de página de reembolsos se limita entre 1 y 300")
    void refundsLimitIsCapped() {
        when(getCommerceAdminStatsPort.findRecentRefunds(300)).thenReturn(List.of());

        assertThat(refunds.getRefunds(1000).getBody().getData()).isEmpty();
        refunds.getRefunds(0);

        verify(getCommerceAdminStatsPort).findRecentRefunds(300);
        verify(getCommerceAdminStatsPort).findRecentRefunds(1);
    }

    @Test
    @WithMockUser(authorities = "ORGANIZER")
    @DisplayName("sin rol ADMIN no se ven reembolsos ni se acredita a todos")
    void nonAdminIsDenied() {
        assertThatThrownBy(() -> refunds.getRefunds(10)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> wallet.bulkCreditAll(null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(getCommerceAdminStatsPort, bulkCreditWalletUseCase);
    }
}
