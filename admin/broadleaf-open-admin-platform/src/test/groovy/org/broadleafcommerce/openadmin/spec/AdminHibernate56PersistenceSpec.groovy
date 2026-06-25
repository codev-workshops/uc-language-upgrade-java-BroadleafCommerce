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

import org.broadleafcommerce.openadmin.server.dao.DynamicEntityDaoImpl
import org.broadleafcommerce.openadmin.server.service.persistence.module.criteria.FieldPathBuilder
import org.hibernate.jpa.QueryHints

import spock.lang.Specification

class AdminHibernate56PersistenceSpec extends Specification {

    def "QueryHints resolves from org.hibernate.jpa package"() {
        expect:
        QueryHints.HINT_CACHEABLE != null
        QueryHints.HINT_CACHE_REGION != null
    }

    def "DynamicEntityDaoImpl can be instantiated"() {
        when:
        def dao = new DynamicEntityDaoImpl()

        then:
        dao != null
    }

    def "FieldPathBuilder can be instantiated and fields set"() {
        when:
        def builder = new FieldPathBuilder()
        builder.setRestrictions(new ArrayList())

        then:
        builder.getRestrictions() != null
        builder.getRestrictions().isEmpty()
    }
}
