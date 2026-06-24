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

/**
 * @author Jeff Fischer
 */
public class RuntimeLog4jConfigurer {

    private String log4jConfigLocation;

    public String getLog4jConfigLocation() {
        return log4jConfigLocation;
    }

    /**
     * Sets the configuration location. Spring's {@code org.springframework.util.Log4jConfigurer} (a Log4j 1.x
     * helper) was removed in Spring 5+; with the migration to Log4j 2 / slf4j 2 logging configuration is
     * resolved by Log4j 2's own bootstrap (e.g. the {@code log4j2.configurationFile} system property), so this
     * setter simply records the location.
     */
    public void setLog4jConfigLocation(String log4jConfigLocation) {
        this.log4jConfigLocation = log4jConfigLocation;
    }
}
