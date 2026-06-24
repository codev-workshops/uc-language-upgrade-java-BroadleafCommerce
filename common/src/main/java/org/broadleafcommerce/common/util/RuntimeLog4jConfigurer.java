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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Jeff Fischer
 */
public class RuntimeLog4jConfigurer {

    private static final Log LOG = LogFactory.getLog(RuntimeLog4jConfigurer.class);

    private String log4jConfigLocation;

    public String getLog4jConfigLocation() {
        return log4jConfigLocation;
    }

    /**
     * Spring 6 removed {@code org.springframework.util.Log4jConfigurer} and the underlying log4j 1.x runtime, and the
     * upgraded stack routes the log4j 1.x API through {@code log4j-over-slf4j} (a logging bridge that cannot be
     * configured from a log4j config file). Runtime log4j configuration is therefore no longer supported here; the
     * configured location is retained for backwards compatibility but is not applied. Configure the active SLF4J
     * backend (e.g. logback or log4j2) through its own configuration mechanism instead.
     */
    public void setLog4jConfigLocation(String log4jConfigLocation) {
        this.log4jConfigLocation = log4jConfigLocation;
        if (log4jConfigLocation != null) {
            LOG.warn("Ignoring log4jConfigLocation '" + log4jConfigLocation + "'. Runtime log4j configuration is no "
                    + "longer supported on the upgraded stack (log4j 1.x is bridged to SLF4J). Configure the active "
                    + "SLF4J backend directly.");
        }
    }
}
