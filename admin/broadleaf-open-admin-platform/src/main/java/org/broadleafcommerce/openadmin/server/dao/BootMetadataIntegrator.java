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
package org.broadleafcommerce.openadmin.server.dao;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures the boot time {@link Metadata} for each {@link SessionFactory}. Hibernate 6 no longer exposes
 * the mapping model through the {@code Configuration}/{@code SessionFactory} once bootstrap has completed,
 * so it is retained here for the metadata driven admin persistence layer.
 *
 * <p>Registered through {@code META-INF/services/org.hibernate.integrator.spi.Integrator}.
 */
public class BootMetadataIntegrator implements Integrator {

  private static final Map<SessionFactory, Metadata> METADATA = new ConcurrentHashMap<>();

  /**
   * Returns the {@link PersistentClass} mapped to the given entity name, or null when the entity is not
   * mapped by the given session factory.
   */
  public static PersistentClass getPersistentClass(SessionFactory sessionFactory, String entityName) {
    Metadata metadata = METADATA.get(sessionFactory);
    return metadata == null ? null : metadata.getEntityBinding(entityName);
  }

  @Override
  public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
      SessionFactoryImplementor sessionFactory) {
    METADATA.put(sessionFactory, metadata);
  }

  @Override
  public void disintegrate(SessionFactoryImplementor sessionFactory,
      SessionFactoryServiceRegistry serviceRegistry) {
    METADATA.remove(sessionFactory);
  }
}
