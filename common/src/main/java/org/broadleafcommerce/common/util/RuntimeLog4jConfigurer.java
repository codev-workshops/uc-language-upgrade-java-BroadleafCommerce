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
package org.broadleafcommerce.common.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.util.ResourceUtils;

import java.net.URL;

/**
 * @author Jeff Fischer
 */
public class RuntimeLog4jConfigurer {

    private String log4jConfigLocation;

    public String getLog4jConfigLocation() {
        return log4jConfigLocation;
    }

    public void setLog4jConfigLocation(String log4jConfigLocation) {
        this.log4jConfigLocation = log4jConfigLocation;
        try {
            URL url = ResourceUtils.getURL(log4jConfigLocation);
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(url.toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
