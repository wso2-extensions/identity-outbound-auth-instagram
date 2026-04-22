/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.json.JSONObject;
import org.wso2.carbon.extension.identity.verification.mgt.IdentityVerificationManager;
import org.wso2.carbon.extension.identity.verification.mgt.exception.IdentityVerificationException;
import org.wso2.carbon.extension.identity.verification.mgt.model.IdVClaim;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectExecutor;
import org.wso2.carbon.identity.authenticator.daon.internal.DaonServiceHolder;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.flow.execution.engine.exception.FlowEngineException;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.USERNAME_CLAIM_URI;

/**
 * Flow executor for Daon identity verification (IDV).
 *
 * <p>Handles the OIDC authorization-code flow against Daon's token endpoint. The returned
 * ID token carries a nested {@code claims} object containing IDV attributes (name, birthdate,
 * document details, address). These are extracted, logged, and surfaced as flow user attributes
 * for the WSO2 self-registration pipeline.</p>
 */
public class DaonExecutor extends OpenIDConnectExecutor {

    private static final Log LOG = LogFactory.getLog(DaonExecutor.class);
    private static final String DAON_EXECUTOR_NAME = "DaonExecutor";

    @Override
    public String getName() {
        return DAON_EXECUTOR_NAME;
    }

    @Override
    public String getAMRValue() {
        return DAON_EXECUTOR_NAME;
    }

    @Override
    public String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {
        String endpoint = authenticatorProperties.get(DaonAuthenticatorConstants.DAON_AUTH_ENDPOINT_PARAM);
        return StringUtils.isNotBlank(endpoint) ? endpoint : DaonAuthenticatorConstants.DAON_OAUTH_ENDPOINT;
    }

    @Override
    public String getTokenEndpoint(Map<String, String> authenticatorProperties) {
        String endpoint = authenticatorProperties.get(DaonAuthenticatorConstants.DAON_TOKEN_ENDPOINT_PARAM);
        return StringUtils.isNotBlank(endpoint) ? endpoint : DaonAuthenticatorConstants.DAON_TOKEN_ENDPOINT;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext flowExecutionContext) {

        flowExecutionContext.setPortalUrl("https://127.0.0.1:9443/accounts/register");
        return super.execute(flowExecutionContext);
    }

    @Override
    public Map<String, String> getAdditionalQueryParams(Map<String, String> authenticatorProperties) {

        //return authenticatorProperties.get(FrameworkConstants.QUERY_PARAMS);
        Map<String, String> claimsMap = new HashMap<>();
        claimsMap.put("claims", "%7B%20%22id_token%22%3A%20%7B%20%22verified_claims%22%3A%20%7B%20%22verification%22%3A%20%7B%20%22trust_framework%22%3A%20%22daon-identify-1%22%20%7D%2C%20%22claims%22%3A%20%7B%20%22family_name_and_given_name%22%3A%20null%2C%20%22birthdate%22%3A%20null%2C%20%22nationality%22%3A%20null%2C%20%22family_name_and_given_name%20%28%2A%29%22%3A%20null%2C%20%22given_name%22%3A%20null%2C%20%22family_name%20%28%2A%29%22%3A%20null%2C%20%22nationality_code%22%3A%20null%2C%20%22family_name%22%3A%20null%2C%20%22given_name%20%28%2A%29%22%3A%20null%2C%20%22first_family_name%20%28%2A%29%22%3A%20null%2C%20%22second_family_name%20%28%2A%29%22%3A%20null%2C%20%22first_family_name%22%3A%20null%2C%20%22second_family_name%22%3A%20null%2C%20%22document_type%22%3A%20null%2C%20%22document_classification%22%3A%20null%2C%20%22document_date_of_expiry%22%3A%20null%2C%20%22document_number%22%3A%20null%2C%20%22document_personal_number%22%3A%20null%2C%20%22address%22%3A%20null%20%7D%20%7D%20%7D%20%7D");
        return claimsMap;
    }

    /**
     * Exchanges the authorization code for tokens, then extracts Daon IDV claims from the
     * nested {@code claims} object inside the ID token JWT payload.
     *
     * <p>All extracted claims are logged and added to the returned attribute map under the
     * Daon claim dialect URI ({@code http://wso2.org/daon/claims/<key>}). The OIDC subject
     * ({@code sub}) is mapped to {@link org.wso2.carbon.identity.flow.execution.engine.Constants#USERNAME_CLAIM_URI}.</p>
     */
    @Override
    protected Map<String, Object> resolveUserAttributes(FlowExecutionContext flowExecutionContext, String code)
            throws FlowEngineException {

        OAuthClientResponse oAuthResponse = requestAccessToken(flowExecutionContext, code);
        resolveAccessToken(oAuthResponse);

        String idToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ID_TOKEN);
        if (StringUtils.isBlank(idToken)) {
            throw handleFlowEngineServerException("ID token is empty or null.", null);
        }

        JSONObject idTokenPayload;
        try {
            idTokenPayload = DaonJwtUtil.decodeJwtPayload(idToken);
        } catch (IllegalArgumentException e) {
            throw handleFlowEngineServerException(e.getMessage(), e);
        }

        String subject = idTokenPayload.optString(DaonAuthenticatorConstants.JWT_SUBJECT_CLAIM, null);
        if (StringUtils.isBlank(subject)) {
            throw handleFlowEngineServerException("Subject (sub) claim not found in Daon ID token.", null);
        }

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put(USERNAME_CLAIM_URI, subject);

        if (!idTokenPayload.has(DaonAuthenticatorConstants.JWT_VERIFIED_CLAIMS_OBJECT)) {
            LOG.warn("No 'verifiedClaims' object in Daon ID token for subject: " + subject);
            return userAttributes;
        }

        JSONObject verifiedClaims = idTokenPayload.getJSONObject(DaonAuthenticatorConstants.JWT_VERIFIED_CLAIMS_OBJECT);
        if (!verifiedClaims.has(DaonAuthenticatorConstants.JWT_CLAIMS_OBJECT)) {
            LOG.warn("No 'claims' object inside 'verifiedClaims' in Daon ID token for subject: " + subject);
            return userAttributes;
        }

        JSONObject daonClaims = verifiedClaims.getJSONObject(DaonAuthenticatorConstants.JWT_CLAIMS_OBJECT);
        Map<String, String> extractedClaims = new HashMap<>();
        for (Object keyObj : daonClaims.keySet()) {
            String key = (String) keyObj;
            String claimValue = DaonJwtUtil.resolveClaimValue(key, daonClaims.get(key));
            if (claimValue == null) {
                continue;
            }
            String claimUri = DaonAuthenticatorConstants.CLAIM_DIALECT_URI + "/" + key;
            extractedClaims.put(claimUri, claimValue);
            userAttributes.put(claimUri, claimValue);
        }

        String userId = flowExecutionContext.getFlowUser() != null
                ? flowExecutionContext.getFlowUser().getUserId() : null;
        persistIdvClaims(userId, flowExecutionContext.getTenantDomain(), extractedClaims);

        return userAttributes;
    }

    private void persistIdvClaims(String userId, String tenantDomain, Map<String, String> claims) {

        if (StringUtils.isBlank(userId)) {
            LOG.warn("User ID is not available in flow context; skipping IDV claim persistence.");
            return;
        }

        IdentityVerificationManager manager = DaonServiceHolder.getInstance().getIdentityVerificationManager();
        if (manager == null) {
            LOG.error("IdentityVerificationManager is unavailable; skipping IDV claim persistence.");
            return;
        }

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        List<IdVClaim> idvClaims = new ArrayList<>();
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            IdVClaim claim = new IdVClaim();
            claim.setUserId(userId);
            claim.setClaimUri(entry.getKey());
            claim.setClaimValue(entry.getValue());
            claim.setIdVPId(DaonAuthenticatorConstants.DAON_IDV_PROVIDER_ID);
            claim.setIsVerified(true);
            idvClaims.add(claim);
        }

        try {
            IdVClaim[] existing = manager.getIdVClaims(userId, DaonAuthenticatorConstants.DAON_IDV_PROVIDER_ID,
                    null, tenantId);
            if (existing != null && existing.length > 0) {
                manager.updateIdVClaims(userId, idvClaims, tenantId);
            } else {
                manager.addIdVClaims(userId, idvClaims, tenantId);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Persisted " + idvClaims.size() + " IDV claim(s) for user: " + userId);
            }
        } catch (IdentityVerificationException e) {
            LOG.error("Failed to persist IDV claims for user: " + userId, e);
        }
    }
}
