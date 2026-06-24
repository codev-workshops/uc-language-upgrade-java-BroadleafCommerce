/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2017 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.common.persistence.transaction;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Support introduction of customized or additional services to the Hibernate service registry.
 *
 * <p>Hibernate 6 removed {@code ServiceContributingIntegrator}; the service-initiator contribution that previously
 * lived in {@code prepareServices} now happens through the {@link ServiceContributor} SPI, while the no-op lifecycle
 * hooks remain on the {@link Integrator} SPI.
 *
 * @author Jeff Fischer
 */
public class CommonServiceIntegrator implements Integrator, ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addInitiator(LifecycleAwareJDBCServicesInitiator.INSTANCE);
    }

    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        //do nothing
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        //do nothing
    }
}
