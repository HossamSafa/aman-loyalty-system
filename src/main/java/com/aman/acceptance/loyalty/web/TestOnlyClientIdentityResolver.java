package com.aman.acceptance.loyalty.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * TEST-ONLY. Active only under the "local-test" Spring profile, so it can
 * NEVER be picked up by default or accidentally in production (see the
 * complementary {@code @Profile("!local-test")} on {@link ClientIdentityResolver}).
 * Returns a fixed clientId purely so the /earnings flow can be exercised
 * end-to-end in Postman while the real authentication layer is still
 * pending (see {@link ClientIdentityResolver} for what that requires).
 * Delete this class once real JWT validation is wired in - do not extend
 * or reuse it for anything beyond local manual testing.
 */
@Component
@Profile("local-test")
public class TestOnlyClientIdentityResolver extends ClientIdentityResolver {

    private static final String FIXED_TEST_CLIENT_ID = "test-client";

    @Override
    public String resolveClientId(HttpServletRequest request) {
        return FIXED_TEST_CLIENT_ID;
    }
}
