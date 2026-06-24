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
import java.util.Map;

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * 
 * @author jfischer
 *
 */
public class EJB3ConfigurationDaoImpl implements EJB3ConfigurationDao {

    private Metadata configuration = null;

    protected PersistenceUnitInfo persistenceUnitInfo;

    public Metadata getConfiguration() {
        synchronized(this) {
            if (configuration == null) {
                String previousValue = persistenceUnitInfo.getProperties().getProperty("hibernate.hbm2ddl.auto");
                persistenceUnitInfo.getProperties().setProperty("hibernate.hbm2ddl.auto", "none");
                // Hibernate 6 replaced Ejb3Configuration with the metadata bootstrap. Building the
                // EntityManagerFactoryBuilder and asking for its metadata() runs the full mapping
                // bind step (producing the PersistentClass bindings) without forcing us to build a
                // SessionFactory just to read the mappings.
                Map<String, Object> integration = new HashMap<String, Object>();
                EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl)
                        Bootstrap.getEntityManagerFactoryBuilder(persistenceUnitInfo, integration);
                configuration = builder.metadata();
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
