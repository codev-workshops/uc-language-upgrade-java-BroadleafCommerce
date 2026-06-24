/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
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
package org.broadleafcommerce.common.email.service.message;

import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Properties;

/**
 * Replacement for Spring's removed {@code org.springframework.ui.velocity.VelocityEngineFactoryBean}, which was
 * dropped along with the rest of Spring's Velocity integration. Builds and initializes an Apache Velocity
 * {@link VelocityEngine} from the supplied properties.
 */
public class VelocityEngineFactoryBean implements FactoryBean<VelocityEngine>, InitializingBean {

    private Properties velocityProperties = new Properties();

    private VelocityEngine velocityEngine;

    public void setVelocityProperties(Properties velocityProperties) {
        this.velocityProperties = velocityProperties;
    }

    public Properties getVelocityProperties() {
        return velocityProperties;
    }

    @Override
    public void afterPropertiesSet() {
        VelocityEngine engine = new VelocityEngine();
        if (velocityProperties != null) {
            for (String key : velocityProperties.stringPropertyNames()) {
                engine.setProperty(key, velocityProperties.getProperty(key));
            }
        }
        engine.init();
        this.velocityEngine = engine;
    }

    @Override
    public VelocityEngine getObject() {
        return velocityEngine;
    }

    @Override
    public Class<?> getObjectType() {
        return VelocityEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
