/*
 * #%L
 * BroadleafCommerce Common Libraries
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
package org.broadleafcommerce.common.email.service.message;

import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Properties;

/**
 * Builds and configures a Velocity 2 ({@code velocity-engine-core}) {@link VelocityEngine}.
 *
 * <p>Spring 5 removed the {@code org.springframework.ui.velocity} support package (including
 * {@code VelocityEngineFactoryBean}) because Apache Velocity reached end-of-life as a Spring view technology. Broadleaf
 * still uses Velocity to render e-mail templates, so this minimal factory bean reproduces the small slice of behavior
 * that was previously provided by Spring: instantiate a {@link VelocityEngine} and initialize it with the supplied
 * properties.
 *
 * @author Broadleaf Commerce (Java 17 migration)
 */
public class VelocityEngineFactoryBean implements FactoryBean<VelocityEngine>, InitializingBean {

    protected Properties velocityProperties = new Properties();

    protected VelocityEngine velocityEngine;

    @Override
    public void afterPropertiesSet() throws Exception {
        VelocityEngine engine = new VelocityEngine();
        if (velocityProperties != null) {
            engine.init(velocityProperties);
        } else {
            engine.init();
        }
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

    public Properties getVelocityProperties() {
        return velocityProperties;
    }

    public void setVelocityProperties(Properties velocityProperties) {
        this.velocityProperties = velocityProperties;
    }

}
