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

public class DaonAuthenticatorConstants {

    // Default Daon (Keycloak) endpoints — configurable via authenticator properties
    public static final String DAON_OAUTH_ENDPOINT =
            "https://wso2.oak.trustx.com/auth/realms/wso2/protocol/openid-connect/auth";
    public static final String DAON_TOKEN_ENDPOINT =
            "https://wso2.oak.trustx.com/auth/realms/wso2/protocol/openid-connect/token";

    // Connector metadata
    public static final String DAON_CONNECTOR_FRIENDLY_NAME = "Daon";
    public static final String DAON_CONNECTOR_NAME = "DaonAuthenticator";

    // Configurable property keys exposed in the admin UI
    public static final String DAON_AUTH_ENDPOINT_PARAM = "daonAuthorizationEndpoint";
    public static final String DAON_TOKEN_ENDPOINT_PARAM = "daonTokenEndpoint";

    // Claim dialect URI for Daon-specific claims
    public static final String CLAIM_DIALECT_URI = "http://wso2.org/daon/claims";

    // Identity verification provider ID registered in WSO2 IS for Daon
    public static final String DAON_IDV_PROVIDER_ID = "DAON";

    // Top-level JWT claim field names
    public static final String JWT_SUBJECT_CLAIM = "sub";
    public static final String JWT_VERIFIED_CLAIMS_OBJECT = "verifiedClaims";
    public static final String JWT_CLAIMS_OBJECT = "claims";

    // Daon claim keys inside the "claims" JWT object
    public static final String CLAIM_ADDRESS = "address";
    public static final String CLAIM_ADDRESS_FORMATTED = "formatted";

    // OIDC claims request parameter sent to the Daon authorization endpoint.
    // Requests all IDV claims inside the id_token using the verified_claims structure.
    public static final String DAON_CLAIMS_REQUEST_JSON =
            "{\"id_token\":{\"verified_claims\":{" +
            "\"verification\":{\"trust_framework\":\"daon-identify-1\"}," +
            "\"claims\":{" +
            "\"family_name_and_given_name\":null," +
            "\"birthdate\":null," +
            "\"nationality\":null," +
            "\"nationality_code\":null," +
            "\"given_name\":null," +
            "\"family_name\":null," +
            "\"first_family_name\":null," +
            "\"second_family_name\":null," +
            "\"document_type\":null," +
            "\"document_classification\":null," +
            "\"document_date_of_expiry\":null," +
            "\"document_number\":null," +
            "\"document_personal_number\":null," +
            "\"address\":null" +
            "}}}}";
}
