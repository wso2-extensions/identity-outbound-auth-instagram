/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.authenticator.daon;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.json.JSONObject;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Federated authenticator for Daon identity verification.
 *
 * <p>Authenticates users via Daon's OIDC-compatible token endpoint. The ID token
 * returned by Daon contains a nested {@code claims} object with identity attributes
 * (name, birthdate, document details, address) that are extracted and mapped to the
 * WSO2 claim dialect {@code http://wso2.org/daon/claims}.</p>
 */
public class DaonAuthenticator extends OpenIDConnectAuthenticator implements FederatedApplicationAuthenticator {

    private static final long serialVersionUID = -1179165995021182757L;
    private static final Log log = LogFactory.getLog(DaonAuthenticator.class);

    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {
        String endpoint = authenticatorProperties.get(DaonAuthenticatorConstants.DAON_AUTH_ENDPOINT_PARAM);
        return StringUtils.isNotBlank(endpoint) ? endpoint : DaonAuthenticatorConstants.DAON_OAUTH_ENDPOINT;
    }

    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {
        String endpoint = authenticatorProperties.get(DaonAuthenticatorConstants.DAON_TOKEN_ENDPOINT_PARAM);
        return StringUtils.isNotBlank(endpoint) ? endpoint : DaonAuthenticatorConstants.DAON_TOKEN_ENDPOINT;
    }

    @Override
    protected String getQueryString(Map<String, String> authenticatorProperties) {

        //return authenticatorProperties.get(FrameworkConstants.QUERY_PARAMS);
        return "%7B%20%22id_token%22%3A%20%7B%20%22verified_claims%22%3A%20%7B%20%22verification%22%3A%20%7B%20%22trust_framework%22%3A%20%22daon-identify-1%22%20%7D%2C%20%22claims%22%3A%20%7B%20%22family_name_and_given_name%22%3A%20null%2C%20%22birthdate%22%3A%20null%2C%20%22nationality%22%3A%20null%2C%20%22family_name_and_given_name%20%28%2A%29%22%3A%20null%2C%20%22given_name%22%3A%20null%2C%20%22family_name%20%28%2A%29%22%3A%20null%2C%20%22nationality_code%22%3A%20null%2C%20%22family_name%22%3A%20null%2C%20%22given_name%20%28%2A%29%22%3A%20null%2C%20%22first_family_name%20%28%2A%29%22%3A%20null%2C%20%22second_family_name%20%28%2A%29%22%3A%20null%2C%20%22first_family_name%22%3A%20null%2C%20%22second_family_name%22%3A%20null%2C%20%22document_type%22%3A%20null%2C%20%22document_classification%22%3A%20null%2C%20%22document_date_of_expiry%22%3A%20null%2C%20%22document_number%22%3A%20null%2C%20%22document_personal_number%22%3A%20null%2C%20%22address%22%3A%20null%20%7D%20%7D%20%7D%20%7D";
    }

    // -----------------------------------------------------------------------
    // Connector metadata
    // -----------------------------------------------------------------------

    @Override
    public String getFriendlyName() {
        return DaonAuthenticatorConstants.DAON_CONNECTOR_FRIENDLY_NAME;
    }

    @Override
    public String getName() {
        return DaonAuthenticatorConstants.DAON_CONNECTOR_NAME;
    }

    @Override
    public String getClaimDialectURI() {
        return DaonAuthenticatorConstants.CLAIM_DIALECT_URI;
    }

    /**
     * Daon returns an ID token; signal the framework that an ID token is expected.
     */
    @Override
    protected boolean requiredIDToken(Map<String, String> authenticatorProperties) {
        return true;
    }

    // -----------------------------------------------------------------------
    // Authentication response processing
    // -----------------------------------------------------------------------

    /**
     * Exchanges the authorisation code for tokens, decodes the Daon ID token (JWT),
     * and populates the authentication context with the subject and claim attributes.
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {
        try {
            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
            String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
            String clientSecret = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_SECRET);
            String tokenEndPoint = getTokenEndpoint(authenticatorProperties);
            String callbackUrl = getCallbackUrl(authenticatorProperties);

            OAuthAuthzResponse authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = authzResponse.getCode();

            OAuthClientRequest accessRequest =
                    buildAccessRequest(tokenEndPoint, clientId, code, clientSecret, callbackUrl);
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthClientResponse oAuthResponse = exchangeCodeForToken(oAuthClient, accessRequest);

            String accessToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN);
            if (StringUtils.isBlank(accessToken)) {
                throw new AuthenticationFailedException("Access token is empty or null");
            }

            String idToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ID_TOKEN);
            if (StringUtils.isBlank(idToken)) {
                throw new AuthenticationFailedException("ID token is empty or null");
            }

            JSONObject idTokenPayload;
            try {
                idTokenPayload = DaonJwtUtil.decodeJwtPayload(idToken);
            } catch (IllegalArgumentException e) {
                throw new AuthenticationFailedException(e.getMessage(), e);
            }

            String subject = idTokenPayload.optString(DaonAuthenticatorConstants.JWT_SUBJECT_CLAIM, null);
            if (StringUtils.isBlank(subject)) {
                throw new AuthenticationFailedException("Subject (sub) claim not found in Daon ID token");
            }

            context.setProperty(OIDCAuthenticatorConstants.ACCESS_TOKEN, accessToken);
            context.setProperty(OIDCAuthenticatorConstants.ID_TOKEN, idToken);

            AuthenticatedUser authenticatedUser =
                    AuthenticatedUser.createFederateAuthenticatedUserFromSubjectIdentifier(subject);
            authenticatedUser.setAuthenticatedSubjectIdentifier(subject);
            authenticatedUser.setUserAttributes(buildClaimMappings(idTokenPayload));
            context.setSubject(authenticatedUser);

        } catch (OAuthProblemException e) {
            throw new AuthenticationFailedException("Authentication process failed", e);
        }
    }

    // -----------------------------------------------------------------------
    // Claim mapping
    // -----------------------------------------------------------------------

    /**
     * Extracts the {@code claims} object from the Daon ID token payload and builds
     * WSO2 {@link ClaimMapping} entries under the Daon claim dialect.
     */
    private Map<ClaimMapping, String> buildClaimMappings(JSONObject idTokenPayload) {
        Map<ClaimMapping, String> claims = new HashMap<>();

        if (!idTokenPayload.has(DaonAuthenticatorConstants.JWT_CLAIMS_OBJECT)) {
            if (log.isDebugEnabled()) {
                log.debug("No 'claims' object found in Daon ID token payload");
            }
            return claims;
        }

        JSONObject daonClaims = idTokenPayload.getJSONObject(DaonAuthenticatorConstants.JWT_CLAIMS_OBJECT);

        for (Object keyObj : daonClaims.keySet()) {
            String key = (String) keyObj;
            String claimValue = DaonJwtUtil.resolveClaimValue(key, daonClaims.get(key));
            if (claimValue == null) {
                continue;
            }
            String claimUri = DaonAuthenticatorConstants.CLAIM_DIALECT_URI + "/" + key;
            claims.put(ClaimMapping.build(claimUri, claimUri, null, false), claimValue);
            if (log.isDebugEnabled()) {
                log.debug("Mapped Daon claim: " + claimUri + " = " + claimValue);
            }
        }
        return claims;
    }

    // -----------------------------------------------------------------------
    // OAuth2 helpers
    // -----------------------------------------------------------------------

    private OAuthClientRequest buildAccessRequest(String tokenEndPoint, String clientId,
                                                   String code, String clientSecret, String callbackUrl)
            throws AuthenticationFailedException {
        try {
            return OAuthClientRequest.tokenLocation(tokenEndPoint)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectURI(callbackUrl)
                    .setCode(code)
                    .buildBodyMessage();
        } catch (OAuthSystemException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }

    private OAuthClientResponse exchangeCodeForToken(OAuthClient oAuthClient, OAuthClientRequest accessRequest)
            throws AuthenticationFailedException {
        try {
            return oAuthClient.accessToken(accessRequest);
        } catch (OAuthSystemException | OAuthProblemException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Admin UI configuration properties
    // -----------------------------------------------------------------------

    @Override
    public List<Property> getConfigurationProperties() {
        List<Property> configProperties = new ArrayList<Property>();

        Property clientId = new Property();
        clientId.setName(OIDCAuthenticatorConstants.CLIENT_ID);
        clientId.setDisplayName("Client Id");
        clientId.setRequired(true);
        clientId.setDescription("Enter Daon client identifier value");
        clientId.setDisplayOrder(0);
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName(OIDCAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setDisplayName("Client Secret");
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Daon client secret value");
        clientSecret.setDisplayOrder(1);
        configProperties.add(clientSecret);

        Property callbackUrl = new Property();
        callbackUrl.setName(IdentityApplicationConstants.OAuth2.CALLBACK_URL);
        callbackUrl.setDisplayName("Callback URL");
        callbackUrl.setDescription("Enter the callback URL");
        callbackUrl.setDisplayOrder(2);
        configProperties.add(callbackUrl);

        Property authEndpoint = new Property();
        authEndpoint.setName(DaonAuthenticatorConstants.DAON_AUTH_ENDPOINT_PARAM);
        authEndpoint.setDisplayName("Authorization Endpoint URL");
        authEndpoint.setRequired(true);
        authEndpoint.setDescription("Daon authorization endpoint, e.g. " +
                DaonAuthenticatorConstants.DAON_OAUTH_ENDPOINT);
        authEndpoint.setDisplayOrder(3);
        configProperties.add(authEndpoint);

        Property tokenEndpoint = new Property();
        tokenEndpoint.setName(DaonAuthenticatorConstants.DAON_TOKEN_ENDPOINT_PARAM);
        tokenEndpoint.setDisplayName("Token Endpoint URL");
        tokenEndpoint.setRequired(true);
        tokenEndpoint.setDescription("Daon token endpoint, e.g. " +
                DaonAuthenticatorConstants.DAON_TOKEN_ENDPOINT);
        tokenEndpoint.setDisplayOrder(4);
        configProperties.add(tokenEndpoint);

        Property additionalParams = new Property();
        additionalParams.setName(IdentityApplicationConstants.Authenticator.OIDC.QUERY_PARAMS);
        additionalParams.setDisplayName("Additional Query Parameters");
        additionalParams.setRequired(false);
        additionalParams.setDescription("Additional query parameters. e.g: paramName1=value1");
        additionalParams.setType("string");
        additionalParams.setDisplayOrder(5);
        configProperties.add(additionalParams);

        return configProperties;
    }
}
