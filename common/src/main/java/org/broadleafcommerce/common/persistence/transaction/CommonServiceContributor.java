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

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Registers the custom {@link LifecycleAwareJDBCServicesInitiator} with the Hibernate service registry.
 *
 * <p>Through Hibernate 4 a service initiator was contributed from inside an
 * {@code org.hibernate.integrator.spi.ServiceContributingIntegrator#prepareServices}. That interface was removed in
 * Hibernate 5; the replacement hook is {@link ServiceContributor}, discovered through
 * {@code META-INF/services/org.hibernate.service.spi.ServiceContributor}.
 *
 * @author Jeff Fischer
 */
public class CommonServiceContributor implements ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addInitiator(LifecycleAwareJDBCServicesInitiator.INSTANCE);
    }
}
