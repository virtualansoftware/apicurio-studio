/*
 * Copyright 2019 JBoss Inc
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

package io.apicurio.hub.api.virtualan;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;
import io.apicurio.hub.api.content.ContentDereferencer;
import io.apicurio.hub.core.config.HubConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * Default implementation of a Virtualan connector.
 *
 * @author elan.thangamani@virtualan.io
 * */

@ApplicationScoped
@Default
public class VirtualanConnector implements IVirtualanConnector {

    private static Logger logger = LoggerFactory.getLogger(VirtualanConnector.class);

    @Inject
    private HubConfiguration config;
    @Inject
    private ContentDereferencer dereferencer;

    /** Virtualan API URL (should ends with /api). */
    private String apiURL;
    private String _keycloakURL;

    /**
     * Create a new connector for interacting with Virtualan.
     */
    public VirtualanConnector() {
    }

    @PostConstruct
    public void postConstruct() {
        String VirtualanURL = config.getVirtualanApiUrl();
        // Store and sanitize Virtualan API URL.
        this.apiURL = VirtualanURL;
        if (!this.apiURL.endsWith("/virtualservices/apis")) {
            this.apiURL += "/virtualservices/apis";
        }
    }

    /**
     * Returns the OAuth token to use when accessing Virtualan.
     *
     * @throws VirtualanConnectorException
     */
    private String getKeycloakOAuthToken() throws VirtualanConnectorException {
//        String keycloakURL = getKeycloakURL();
//        String keycloakClientId = config.getVirtualanClientId();
//        String keycloakClientSecret = config.getVirtualanClientSecret();
//
//        // Retrieve a token using client_credentials flow.
//        HttpRequestWithBody tokenRequest = Unirest.post(keycloakURL + "/protocol/openid-connect/token")
//                .header("Content-Type", "application/x-www-form-urlencoded")
//                .header("Accept", "application/json").basicAuth(keycloakClientId, keycloakClientSecret);
//
//        HttpResponse<JsonNode> tokenResponse = null;
//        try {
//            tokenResponse = tokenRequest.body("grant_type=client_credentials").asJson();
//        } catch (UnirestException e) {
//            logger.error("Exception while connecting to Keycloak backend", e);
//            throw new VirtualanConnectorException(
//                    "Exception while connecting Virtualan Keycloak backend. Check Keycloak configuration.");
//        }
//
//        if (tokenResponse.getStatus() != 200) {
//            logger.error(
//                    "OAuth token cannot be retrieved for Virtualan server, check keycloakClient configuration");
//            throw new VirtualanConnectorException(
//                    "OAuth token cannot be retrieved for Virtualan. Check keycloakClient.");
//        }
        return "NO_TOKEN";//tokenResponse.getBody().getObject().getString("access_token");
    }

    /**
     * Figures out the URL of the Keycloak server that is protecting Virtualan.
     */
    private String getKeycloakURL() throws VirtualanConnectorException {
        if (this._keycloakURL == null) {
            // Retrieve the Keycloak configuration to build keycloakURL.
            HttpResponse<JsonNode> keycloakConfig = null;
            try {
                keycloakConfig = Unirest.get(this.apiURL + "/keycloak/config")
                        .header("Accept", "application/json").asJson();
            } catch (UnirestException e) {
                logger.error("Exception while connecting to Virtualan backend", e);
                throw new VirtualanConnectorException(
                        "Exception while connecting Virtualan backend. Check URL.");
            }

            if (keycloakConfig.getStatus() != 200) {
                logger.error("Keycloak config cannot be fetched from Virtualan server, check configuration");
                throw new VirtualanConnectorException(
                        "Keycloak configuration cannot be fetched from Virtualan. Check URL.");
            }
            String authServer = keycloakConfig.getBody().getObject().getString("auth-server-url");
            String realmName = keycloakConfig.getBody().getObject().getString("realm");
            this._keycloakURL = authServer + "/realms/" + realmName;
        }
        return this._keycloakURL;
    }

    /**
     * Upload an OAS v3 specification content to Virtualan. This will trigger service discovery and mock
     * endpoint publication on the Virtualan side.
     *
     * @param content OAS v3 specification content
     * @throws VirtualanConnectorException if upload fails for many reasons
     */
    public String uploadResourceContent(String content) throws VirtualanConnectorException {
        String oauthToken = this.getKeycloakOAuthToken();
//        try {
//            content = dereferencer.dereference(content);
//        } catch (IOException e) {
//            logger.error("Could not dereference imports in specification content", e);
//            throw new VirtualanConnectorException("Could not dereference imports before sending to Virtualan");
//        }
        MultipartBody uploadRequest = Unirest.post(this.apiURL )
                //.header("Authorization", "Bearer " + oauthToken)
                .field("openApiUrl", new ByteArrayInputStream(content.getBytes(Charset.forName("UTF-8"))), "open-api-contract.yml");

        HttpResponse<String> response = null;
        try {
            response = uploadRequest.asString();
        } catch (UnirestException e) {
            logger.error("Exception while connecting to Virtualan backend", e);
            throw new VirtualanConnectorException("Exception while connecting Virtualan backend. Check URL.");
        }

        switch (response.getStatus()) {
            case 200:
                String serviceRef = response.getBody();
                logger.info("Virtualan mocks have been created/updated for " + serviceRef);
                return serviceRef;
            case 204:
                logger.warn("NoContent returned by Virtualan server");
                throw new VirtualanConnectorException(
                        "NoContent returned by Virtualan server is unexpected return");
            case 400:
                logger.error(
                        "ClientRequestMalformed returned by Virtualan server: " + response.getStatusText());
                throw new VirtualanConnectorException("ClientRequestMalformed returned by Virtualan server");
            case 500:
                logger.error("InternalServerError returned by Virtualan server");
                throw new VirtualanConnectorException("InternalServerError returned by Virtualan server");
            default:
                logger.error("Unexpected response from Virtualan server: " + response.getStatusText());
                throw new VirtualanConnectorException(
                        "Unexpected response by Virtualan server: " + response.getStatusText());
        }
    }

    /**
     * Reserved for future usage.
     *
     * @return List of repository secrets managed by Virtualan server
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public Collection<VirtualanSecret> getSecrets() throws VirtualanConnectorException {
        return null;
    }

    /**
     * Reserved for future usage.
     *
     * @return List of import jobs managed by Virtualan server
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public Collection<VirtualanImporter> getImportJobs() throws VirtualanConnectorException {
        return null;
    }

    /**
     * Reserved for future usage.
     *
     * @param job Import job to create in Virtualan server.
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public void createImportJob(VirtualanImporter job) throws VirtualanConnectorException {
        throw new VirtualanConnectorException("Not implemented");
    }

    /**
     * Reserved for future usage.
     *
     * @param job Import job to force import in Virtualan server.
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public void forceResourceImport(VirtualanImporter job) throws VirtualanConnectorException {
        throw new VirtualanConnectorException("Not implemented");
    }
}
