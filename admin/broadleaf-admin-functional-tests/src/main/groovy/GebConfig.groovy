/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver

import geb.buildadapter.SystemPropertiesBuildAdapter


println 'Loading default Broadleaf GebConfig'
// Use the FirefoxDriver by default
driver = { new FirefoxDriver() }
if (!System.getProperty(SystemPropertiesBuildAdapter.BASE_URL_PROPERTY_NAME)) {
    baseUrl = 'http://demo75ip2w.blcqa.com/admin/'
}

if (!System.getProperty(SystemPropertiesBuildAdapter.REPORTS_DIR_PROPERTY_NAME)) {
    reportsDir = 'target/gebreports'
}
waiting {
    timeout = 60
}
environments {

    // Selenium Manager (Selenium 4.6+) resolves and downloads the matching browser driver
    chrome {
        driver = { new ChromeDriver() }
    }

    firefox {
        driver = { new FirefoxDriver() }
    }

}
