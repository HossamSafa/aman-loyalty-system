package com.aman.acceptance.loyalty.web;

import com.aman.acceptance.loyalty.exception.ClientIdentityUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * BLOCKED - pending real authentication integration. Do not implement a
 * workaround here; see notes below before touching this class.
 * <p>
 * Confirmed by inspection (no false starts): this project has no
 * authentication/security layer at all - no Spring Security dependency, no
 * {@code SecurityFilterChain}, no {@code JwtDecoder}/OAuth2 resource
 * server, no {@code @AuthenticationPrincipal} usage, and no request
 * filter/interceptor that populates a security context or principal
 * anywhere in this codebase. There is therefore no existing authenticated
 * identity this class can read.
 * <p>
 * The design doc (Solution Design v1.0, sections 5.1 and 17.2) specifies
 * that callers present {@code Authorization: Bearer <JWT>} and that the API
 * Gateway authenticates it, but it does not specify:
 * <ul>
 *   <li>an issuer or JWKS endpoint this service could use to verify a
 *       token's signature itself, or</li>
 *   <li>which JWT claim represents the caller/client identity.</li>
 * </ul>
 * A previous version of this class decoded the JWT payload without
 * verifying its signature and guessed the claim name
 * ({@code client_id -> azp -> clientId -> sub}). That is insecure (an
 * unverified JWT must never be treated as authenticated) and was removed.
 * <p>
 * Until the real authentication layer exists (e.g.
 * {@code spring-boot-starter-oauth2-resource-server} configured with a
 * concrete issuer-uri/JWKS, exposing a validated {@code Jwt}/
 * {@code Authentication} with an agreed client-identity claim), this method
 * fails closed with {@link ClientIdentityUnavailableException} rather than
 * trusting anything unverified. {@link com.aman.acceptance.loyalty.service.EarningService}
 * already accepts {@code clientId} as a plain parameter, so once
 * authentication is wired in, only this class's method body needs to
 * change - no other code in the earning flow needs to move.
 */
@Component
@Profile("!local-test")
public class ClientIdentityResolver {

    public String resolveClientId(HttpServletRequest request) {
        throw new ClientIdentityUnavailableException();
    }
}
