package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    private Integer permissionId;
    private String code;
    private String name;
    private String description;
}
