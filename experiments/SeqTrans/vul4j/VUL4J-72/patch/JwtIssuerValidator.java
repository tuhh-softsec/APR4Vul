/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.jwt;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.util.Assert;

/**
 * Validates the "iss" claim in a {@link Jwt}, that is matches a configured value
 *
 * @author Josh Cummings
 * @since 5.1
 */
public final class JwtIssuerValidator implements OAuth2TokenValidator<Jwt> {
	private static OAuth2Error INVALID_ISSUER =
			new OAuth2Error(
					OAuth2ErrorCodes.INVALID_REQUEST,
					"This iss claim is not equal to the configured issuer",
					"https://tools.ietf.org/html/rfc6750#section-3.1");

	private final URL issuer;

	/**
	 * Constructs a {@link JwtIssuerValidator} using the provided parameters
	 *
	 * @param issuer - The issuer that each {@link Jwt} should have.
	 */
	public JwtIssuerValidator(String issuer) {
		Assert.notNull(issuer, "issuer cannot be null");

		try {
			this.issuer = new URL(issuer);
		} catch (MalformedURLException ex) {
			throw new IllegalArgumentException(
					"Invalid Issuer URL " + issuer + " : " + ex.getMessage(),
					ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Assert.notNull(token, "token cannot be null");

		if (this.issuer.equals(token.getIssuer())) {
			return OAuth2TokenValidatorResult.success();
		} else {
			return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
		}
	}
}
