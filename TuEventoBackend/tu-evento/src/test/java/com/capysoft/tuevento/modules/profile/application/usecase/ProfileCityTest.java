package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.profile.application.dto.request.UpdateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.mapper.ProfileResponseAssembler;
import com.capysoft.tuevento.modules.profile.application.port.out.CityLookupPort;
import com.capysoft.tuevento.modules.profile.application.port.out.CitySummary;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La ciudad del perfil se guarda como id y se resuelve por {@link CityLookupPort}; la respuesta
 * la arma {@link ProfileResponseAssembler} (real en estas pruebas, con el puerto simulado).
 */
@DisplayName("Ciudad del perfil")
class ProfileCityTest {

    private static final CitySummary MEDELLIN = new CitySummary(3, "Medellín", "Antioquia");

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final CityLookupPort cityLookupPort = mock(CityLookupPort.class);
    private final ProfileResponseAssembler assembler = new ProfileResponseAssembler(cityLookupPort);

    private static Profile profile(Integer cityId) {
        return Profile.builder().profileId(10L).userId(1).fullName("Ana").bio("Hola")
                .storedFileId(4).cityId(cityId).build();
    }

    @Nested
    @DisplayName("al consultar")
    class Read {

        private final GetProfileUseCase useCase = new GetProfileUseCase(profileRepository, assembler);

        @Test
        @DisplayName("muestra los datos del perfil con nombre y departamento de la ciudad")
        void withCity() {
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile(3)));
            when(cityLookupPort.findCity(3)).thenReturn(Optional.of(MEDELLIN));

            ProfileResponse response = useCase.getById(10L);

            assertThat(response.getProfileId()).isEqualTo(10L);
            assertThat(response.getFullName()).isEqualTo("Ana");
            assertThat(response.getBio()).isEqualTo("Hola");
            assertThat(response.getStoredFileId()).isEqualTo(4);
            assertThat(response.getCityId()).isEqualTo(3);
            assertThat(response.getCityName()).isEqualTo("Medellín");
            assertThat(response.getDepartmentName()).isEqualTo("Antioquia");
        }

        @Test
        @DisplayName("si la ciudad no se puede resolver conserva el id y deja los nombres vacíos")
        void cityUnresolved() {
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile(3)));
            when(cityLookupPort.findCity(3)).thenReturn(Optional.empty());

            ProfileResponse response = useCase.getById(10L);

            assertThat(response.getCityId()).isEqualTo(3);
            assertThat(response.getCityName()).isNull();
            assertThat(response.getDepartmentName()).isNull();
        }
    }

    @Nested
    @DisplayName("al editar")
    class Update {

        private final ProfileLogRepository profileLogRepository = mock(ProfileLogRepository.class);
        private final UpdateProfileUseCase useCase = new UpdateProfileUseCase(profileRepository,
                profileLogRepository, cityLookupPort, assembler, mock(ApplicationEventPublisher.class));

        private UpdateProfileRequest cityRequest(int cityId) {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setCityId(cityId);
            return request;
        }

        @Test
        @DisplayName("una ciudad válida se guarda como id y se devuelve con su nombre")
        void validCity() {
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile(null)));
            when(cityLookupPort.getCity(3)).thenReturn(MEDELLIN);
            when(cityLookupPort.findCity(3)).thenReturn(Optional.of(MEDELLIN));
            when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProfileResponse response = useCase.update(10L, cityRequest(3));

            assertThat(response.getCityId()).isEqualTo(3);
            assertThat(response.getCityName()).isEqualTo("Medellín");
            verify(profileLogRepository).save(any());
        }

        @Test
        @DisplayName("una ciudad inexistente da CITY_NOT_FOUND y no guarda")
        void unknownCity() {
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile(1)));
            when(cityLookupPort.getCity(99)).thenThrow(new NotFoundException("CITY_NOT_FOUND", "no"));

            assertThatThrownBy(() -> useCase.update(10L, cityRequest(99)))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("code", "CITY_NOT_FOUND");
            verify(profileRepository, never()).save(any());
        }
    }
}
