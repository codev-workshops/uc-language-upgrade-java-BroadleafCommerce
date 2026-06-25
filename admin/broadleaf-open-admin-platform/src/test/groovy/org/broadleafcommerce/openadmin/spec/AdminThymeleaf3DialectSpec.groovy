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
package org.broadleafcommerce.openadmin.spec

import org.broadleafcommerce.openadmin.web.processor.AdminComponentIdProcessor
import org.broadleafcommerce.openadmin.web.processor.AdminFieldBuilderProcessor
import org.broadleafcommerce.openadmin.web.processor.AdminModuleProcessor
import org.broadleafcommerce.openadmin.web.processor.AdminSectionHrefProcessor
import org.broadleafcommerce.openadmin.web.processor.AdminUserProcessor
import org.broadleafcommerce.openadmin.web.processor.ErrorsProcessor
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor
import org.thymeleaf.processor.element.AbstractElementTagProcessor

import spock.lang.Specification

class AdminThymeleaf3DialectSpec extends Specification {

    def "AdminComponentIdProcessor extends TL3 AbstractAttributeTagProcessor"() {
        when:
        def processor = new AdminComponentIdProcessor()

        then:
        processor instanceof AbstractAttributeTagProcessor
    }

    def "AdminFieldBuilderProcessor extends TL3 AbstractElementTagProcessor"() {
        when:
        def processor = new AdminFieldBuilderProcessor()

        then:
        processor instanceof AbstractElementTagProcessor
    }

    def "AdminModuleProcessor extends TL3 AbstractElementTagProcessor"() {
        when:
        def processor = new AdminModuleProcessor()

        then:
        processor instanceof AbstractElementTagProcessor
    }

    def "AdminSectionHrefProcessor extends TL3 AbstractAttributeTagProcessor"() {
        when:
        def processor = new AdminSectionHrefProcessor()

        then:
        processor instanceof AbstractAttributeTagProcessor
    }

    def "AdminUserProcessor extends TL3 AbstractElementTagProcessor"() {
        when:
        def processor = new AdminUserProcessor()

        then:
        processor instanceof AbstractElementTagProcessor
    }

    def "ErrorsProcessor extends TL3 AbstractAttributeTagProcessor"() {
        when:
        def processor = new ErrorsProcessor()

        then:
        processor instanceof AbstractAttributeTagProcessor
    }
}
