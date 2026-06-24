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

import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Holds the Hibernate boot-time {@link Metadata} for a persistence unit.
 *
 * <p>Under Hibernate 4 this lazily built an {@code Ejb3Configuration} from the {@link PersistenceUnitInfo}.
 * In Hibernate 5+ {@code Ejb3Configuration} was removed and the mapping metadata is produced by the standard
 * bootstrap; the resulting {@link Metadata} is captured during bootstrap (see
 * {@code org.broadleafcommerce.common.persistence.transaction.CommonServiceIntegrator}) and supplied here via
 * {@link #setConfiguration(Metadata)}.</p>
 *
 * @author jfischer
 *
 */
public class EJB3ConfigurationDaoImpl implements EJB3ConfigurationDao {

    private Metadata configuration = null;

    protected PersistenceUnitInfo persistenceUnitInfo;

    public Metadata getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Metadata configuration) {
        this.configuration = configuration;
    }

    public PersistenceUnitInfo getPersistenceUnitInfo() {
        return persistenceUnitInfo;
    }

    public void setPersistenceUnitInfo(PersistenceUnitInfo persistenceUnitInfo) {
        this.persistenceUnitInfo = persistenceUnitInfo;
    }
    
}
