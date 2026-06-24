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

/**
 * Provides access to the Hibernate boot-time {@link Metadata} (mapping information).
 *
 * <p>Under Hibernate 4 this exposed an {@code org.hibernate.ejb.Ejb3Configuration}. That type was removed in
 * Hibernate 5+; the equivalent mapping metadata is now represented by {@link Metadata}, whose
 * {@code getEntityBinding(String)} replaces the former {@code Configuration#getClassMapping(String)}.</p>
 *
 * @author jfischer
 *
 */
public interface EJB3ConfigurationDao {

    public abstract Metadata getConfiguration();

}
