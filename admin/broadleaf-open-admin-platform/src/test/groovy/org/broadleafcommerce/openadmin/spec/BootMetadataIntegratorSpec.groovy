/*
 * #%L
 * BroadleafCommerce Open Admin Platform
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
package org.broadleafcommerce.openadmin.spec

import org.broadleafcommerce.openadmin.server.dao.BootMetadataIntegrator
import org.hibernate.SessionFactory
import org.hibernate.integrator.spi.Integrator

import spock.lang.Specification

/**
 * The Hibernate 4 {@code EJB3ConfigurationDao} used to supply the boot time mapping model to the admin
 * persistence layer. Hibernate 6 exposes it only to an {@link Integrator}, so verify the replacement is
 * discoverable and null safe.
 */
class BootMetadataIntegratorSpec extends Specification {

    def "the integrator is registered for hibernate service loading"() {
        when:
        def integrators = ServiceLoader.load(Integrator).iterator().toList()

        then:
        integrators.any { it instanceof BootMetadataIntegrator }
    }

    def "an unregistered session factory resolves no persistent class"() {
        given:
        SessionFactory sessionFactory = Mock(SessionFactory)

        expect:
        BootMetadataIntegrator.getPersistentClass(sessionFactory, "com.example.NotAnEntity") == null
    }
}
