package com.capysoft.geolocation.domain.exception;

public class SiteAlreadyExistsException extends RuntimeException {

    public SiteAlreadyExistsException(Integer siteId, String siteName) {
        super("A site already exists near this location: " + siteName + " (id: " + siteId + ")");
    }
}
