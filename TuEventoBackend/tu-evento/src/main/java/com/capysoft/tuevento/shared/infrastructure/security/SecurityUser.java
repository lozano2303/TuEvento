package com.capysoft.tuevento.shared.infrastructure.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

@Getter
public class SecurityUser implements UserDetails {

    private final Integer userId;
    private final String  alias;
    private final String  role;

    public SecurityUser(Integer userId, String alias, String role) {
        this.userId = userId;
        this.alias  = alias;
        this.role   = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword()                    { return null; }
    @Override public String getUsername()                    { return alias; }
    @Override public boolean isAccountNonExpired()           { return true; }
    @Override public boolean isAccountNonLocked()            { return true; }
    @Override public boolean isCredentialsNonExpired()       { return true; }
    @Override public boolean isEnabled()                     { return true; }
}
