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

package org.wso2.carbon.identity.authenticator.daon.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.extension.identity.verification.mgt.IdentityVerificationManager;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.authenticator.daon.DaonAuthenticator;
import org.wso2.carbon.identity.authenticator.daon.DaonExecutor;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;

@Component(
        name = "identity.application.authenticator.Daon.component",
        immediate= true)
public class DaonAuthenticatorServiceComponent {

    private static final Log log = LogFactory.getLog(DaonAuthenticatorServiceComponent.class);

    @Activate
    protected void activate(ComponentContext ctxt) {
        try {
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    new DaonAuthenticator(), null);
            ctxt.getBundleContext().registerService(Executor.class.getName(),
                    new DaonExecutor(), null);
            if (log.isDebugEnabled()) {
                log.debug("Daon authenticator and executor are activated");
            }
        } catch (Throwable e) {
            log.fatal("Error while activating the Daon authenticator ", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Daon authenticator is deactivated");
        }
    }

    @Reference(
            name = "identity.verification.manager",
            service = IdentityVerificationManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityVerificationManager"
    )
    protected void setIdentityVerificationManager(IdentityVerificationManager manager) {
        DaonServiceHolder.getInstance().setIdentityVerificationManager(manager);
        log.debug("IdentityVerificationManager bound to Daon connector.");
    }

    protected void unsetIdentityVerificationManager(IdentityVerificationManager manager) {
        DaonServiceHolder.getInstance().setIdentityVerificationManager(null);
        log.debug("IdentityVerificationManager unbound from Daon connector.");
    }
}
