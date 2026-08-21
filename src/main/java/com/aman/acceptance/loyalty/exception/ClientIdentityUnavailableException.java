package com.aman.acceptance.loyalty.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown by {@link com.aman.acceptance.loyalty.web.ClientIdentityResolver}
 * while this service has no authentication layer to derive a trustworthy
 * clientId from. HTTP 501 (not 401/403) because the caller did nothing
 * wrong - the service itself is not yet able to authenticate anyone.
 */
public class ClientIdentityUnavailableException extends LoyaltyException {

    public ClientIdentityUnavailableException() {
        super(
                "LOYALTY_CLIENT_IDENTITY_NOT_IMPLEMENTED",
                HttpStatus.NOT_IMPLEMENTED,
                "Authenticated client identity resolution is not implemented yet. "
                        + "This service has no authentication layer (JWT signature/issuer "
                        + "validation) and no agreed client-identity claim. "
                        + "See ClientIdentityResolver for what is needed before this can be resolved.",
                false
        );
    }
}
