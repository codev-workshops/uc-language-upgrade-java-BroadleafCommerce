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

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

/**
 * {@link StandardServiceInitiator} implementation for introducing the custom {@link JdbcServices} implementation
 * to the Hibernate service registry. As of Hibernate 5 {@code BasicServiceInitiator} was replaced by
 * {@link StandardServiceInitiator}.
 *
 * @author Jeff Fischer
 */
public class LifecycleAwareJDBCServicesInitiator implements StandardServiceInitiator<JdbcServices> {

    public static final LifecycleAwareJDBCServicesInitiator INSTANCE = new LifecycleAwareJDBCServicesInitiator();

    @Override
    public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new LifecycleAwareJDBCServices();
    }

    @Override
    public Class<JdbcServices> getServiceInitiated() {
        return JdbcServices.class;
    }
}
