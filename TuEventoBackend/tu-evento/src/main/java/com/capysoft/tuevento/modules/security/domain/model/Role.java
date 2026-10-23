package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    private Integer roleId;
    private String code;
    private String name;
    private String description;
    private List<Permission> permissions;
}
