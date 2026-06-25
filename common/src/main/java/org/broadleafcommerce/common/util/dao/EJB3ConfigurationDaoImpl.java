/*
 * #%L
 * BroadleafCommerce Open Admin Platform
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
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
package org.broadleafcommerce.common.util.dao;

import org.hibernate.boot.Metadata;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;

import java.util.HashMap;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Builds the Hibernate {@link Metadata} for the persistence unit.
 *
 * <p>As of Hibernate 5 the removed {@code org.hibernate.ejb.Ejb3Configuration} is replaced by the JPA bootstrap
 * pipeline ({@link Bootstrap}/{@link EntityManagerFactoryBuilderImpl}), which assembles the same mapping metadata
 * exposed here as {@link Metadata}.
 *
 * @author jfischer
 */
public class EJB3ConfigurationDaoImpl implements EJB3ConfigurationDao {

    private Metadata configuration = null;

    protected PersistenceUnitInfo persistenceUnitInfo;

    public Metadata getConfiguration() {
        synchronized(this) {
            if (configuration == null) {
                String previousValue = persistenceUnitInfo.getProperties().getProperty("hibernate.hbm2ddl.auto");
                persistenceUnitInfo.getProperties().setProperty("hibernate.hbm2ddl.auto", "none");
                EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap
                        .getEntityManagerFactoryBuilder(persistenceUnitInfo, new HashMap());
                builder.build();
                configuration = builder.getMetadata();
                if (previousValue != null) {
                    persistenceUnitInfo.getProperties().setProperty("hibernate.hbm2ddl.auto", previousValue);
                }
            }
        }
        return configuration;
    }

    public PersistenceUnitInfo getPersistenceUnitInfo() {
        return persistenceUnitInfo;
    }

    public void setPersistenceUnitInfo(PersistenceUnitInfo persistenceUnitInfo) {
        this.persistenceUnitInfo = persistenceUnitInfo;
    }
    
}
