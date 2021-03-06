/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.wls.embedded_12_1;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.wls.ShrinkWrapUtil;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * A {@link DeployableContainer} implementation that uses the {@link EJBContainer} API to control an embedded WLS 12c container.
 * 
 * @author Vineet Reynolds
 */
public class WebLogicContainer implements DeployableContainer<WebLogicEmbeddedConfiguration> {

    private static final Logger logger = Logger.getLogger(WebLogicContainer.class.getName());
    private EJBContainer ejbContainer;
    private WebLogicEmbeddedConfiguration configuration;

    @Inject
    @ContainerScoped
    private InstanceProducer<Context> ctx;

    @Override
    public Class<WebLogicEmbeddedConfiguration> getConfigurationClass() {
        return WebLogicEmbeddedConfiguration.class;
    }

    @Override
    public void setup(WebLogicEmbeddedConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        logger.log(Level.FINE, "Starting container - initialization system properties.");
        if (configuration.isOutputToConsole()) {
            System.setProperty("weblogic.server.embed.debug", "true");
            System.setProperty("weblogic.StdoutDebugEnabled", "true");
        }
    }

    @Override
    public void stop() throws LifecycleException {
        logger.log(Level.FINE, "Starting container - nothing to do here.");
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Local");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        if (ejbContainer != null) {
            throw new DeploymentException("The embedded container does not support multiple deployments in a single test.");
        }
        logger.log(Level.FINE, "Deploying archive {0}", archive);
        // Write the deployment to disk
        File deployment = ShrinkWrapUtil.toFile(archive);
        String deploymentName = getDeploymentName(archive);

        // Prepare embedded container configuration
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EJBContainer.APP_NAME, deploymentName);
        props.put(EJBContainer.MODULES, deployment);

        // Start the embedded container
        ejbContainer = EJBContainer.createEJBContainer(props);
        ctx.set(ejbContainer.getContext());
        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        logger.log(Level.FINE, "Undeploying archive {0}", archive);
        // Stop the embedded container, since there is no undeploy API.
        // To prevent multiple deployment in the same test class scope , we'll close and nullify on undeploy.
        if (ejbContainer != null) {
            ejbContainer.close();
            ejbContainer = null;
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String getDeploymentName(Archive<?> archive) {
        String archiveFilename = archive.getName();
        int indexOfDot = archiveFilename.indexOf(".");
        if (indexOfDot != -1) {
            return archiveFilename.substring(0, indexOfDot);
        }
        return archiveFilename;
    }

}
