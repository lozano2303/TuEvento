package com.capysoft.tuevento.modules.profile.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

    @NotNull
    private Integer userId;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    private Integer storedFileId;

    /**
     * Indicates whether the caller explicitly set storedFileId (even if null).
     * Used to distinguish "use default avatar" from "explicitly set no avatar".
     */
    private boolean storedFileIdSet = false;

    public static CreateProfileRequestBuilder builder() {
        return new CreateProfileRequestBuilder();
    }

    public static class CreateProfileRequestBuilder {
        private Integer userId;
        private String fullName;
        private Integer storedFileId;
        private boolean storedFileIdSet = false;

        public CreateProfileRequestBuilder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public CreateProfileRequestBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public CreateProfileRequestBuilder storedFileId(Integer storedFileId) {
            this.storedFileId = storedFileId;
            this.storedFileIdSet = true;
            return this;
        }

        public CreateProfileRequest build() {
            CreateProfileRequest req = new CreateProfileRequest();
            req.userId = this.userId;
            req.fullName = this.fullName;
            req.storedFileId = this.storedFileId;
            req.storedFileIdSet = this.storedFileIdSet;
            return req;
        }
    }
}
