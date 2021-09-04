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

import java.util.Collection;

/**
 * A Virtualan specific connector.
 * 
 * @author elan.thangamani@virtualan.io
 */
public interface IVirtualanConnector {

    /**
     * Upload an OAS v3 specification content to Virtualan. This will trigger service discovery and mock
     * endpoint publication on the Virtualan side.
     * 
     * @param content OAS v3 specification content
     * @throws VirtualanConnectorException if upload fails for any reasons
     */
    public String uploadResourceContent(String content) throws VirtualanConnectorException;

    /**
     * Reserved for future usage.
     * 
     * @return List of repository secrets managed by Virtualan server
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public Collection<VirtualanSecret> getSecrets() throws VirtualanConnectorException;

    /**
     * Reserved for future usage.
     * 
     * @return List of import jobs managed by Virtualan server
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public Collection<VirtualanImporter> getImportJobs() throws VirtualanConnectorException;

    /**
     * Reserved for future usage.
     * 
     * @param job Import job to create in Virtualan server.
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public void createImportJob(VirtualanImporter job) throws VirtualanConnectorException;

    /**
     * Reserved for future usage.
     * 
     * @param job Import job to force import in Virtualan server.
     * @throws VirtualanConnectorException if connection fails for any reasons
     */
    public void forceResourceImport(VirtualanImporter job) throws VirtualanConnectorException;
}
