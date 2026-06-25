/*
 * #%L
 * BroadleafCommerce Framework
 * %%
 * Copyright (C) 2009 - 2026 Broadleaf Commerce
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
package org.broadleafcommerce.core.spec.search.service.solr

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrInputDocument
import org.broadleafcommerce.common.locale.service.LocaleService
import org.broadleafcommerce.common.sandbox.SandBoxHelper
import org.broadleafcommerce.core.catalog.dao.ProductDao
import org.broadleafcommerce.core.catalog.dao.SkuDao
import org.broadleafcommerce.core.search.dao.FieldDao
import org.broadleafcommerce.core.search.dao.SolrIndexDao
import org.broadleafcommerce.core.search.service.solr.SolrContext
import org.broadleafcommerce.core.search.service.solr.SolrHelperService
import org.broadleafcommerce.core.search.service.solr.SolrIndexServiceImpl
import org.broadleafcommerce.core.search.service.solr.SolrSearchServiceExtensionHandler
import org.broadleafcommerce.core.search.service.solr.SolrSearchServiceExtensionManager
import org.springframework.transaction.PlatformTransactionManager

import spock.lang.Specification

/**
 * Verifies Solr indexing and search works with SolrJ 8.x client API.
 * Ensures that SolrClient (formerly SolrServer) types are correctly used.
 */
class SolrClientMigrationSpec extends Specification {

    SolrIndexServiceImpl service
    SolrIndexDao mockSolrIndexDao = Mock()
    FieldDao mockFieldDao = Mock()
    PlatformTransactionManager mockTransactionManager = Mock()
    ProductDao mockProductDao = Mock()
    SkuDao mockSkuDao = Mock()
    LocaleService mockLocaleService = Mock()
    SolrHelperService mockShs = Mock()
    SolrSearchServiceExtensionManager mockExtensionManager = Mock()
    SandBoxHelper mockSandBoxHelper = Mock()

    def setup() {
        mockLocaleService.findAllLocales() >> new ArrayList<Locale>()
        mockExtensionManager.getProxy() >> Mock(SolrSearchServiceExtensionHandler)

        service = Spy(SolrIndexServiceImpl)
        service.solrIndexDao = mockSolrIndexDao
        service.fieldDao = mockFieldDao
        service.transactionManager = mockTransactionManager
        service.productDao = mockProductDao
        service.skuDao = mockSkuDao
        service.localeService = mockLocaleService
        service.shs = mockShs
        service.extensionManager = mockExtensionManager
        service.sandBoxHelper = mockSandBoxHelper
    }

    def "SolrContext should accept SolrClient instances for primary server"() {
        setup:
        SolrClient mockClient = Mock(SolrClient)

        when:
        SolrContext.setPrimaryServer(mockClient)

        then:
        SolrContext.getServer() == mockClient

        cleanup:
        SolrContext.setPrimaryServer(null)
    }

    def "SolrContext should accept SolrClient instances for reindex server"() {
        setup:
        SolrClient mockClient = Mock(SolrClient)

        when:
        SolrContext.setReindexServer(mockClient)

        then:
        SolrContext.getReindexServer() == mockClient

        cleanup:
        SolrContext.setReindexServer(null)
        SolrContext.setPrimaryServer(null)
    }

    def "SolrContext getAdminServer should return primary when admin not set"() {
        setup:
        SolrClient mockClient = Mock(SolrClient)
        SolrContext.setPrimaryServer(mockClient)

        when:
        SolrClient admin = SolrContext.getAdminServer()

        then:
        admin == mockClient

        cleanup:
        SolrContext.setPrimaryServer(null)
    }

    def "SolrContext isSingleCoreMode should return true when reindex server is null"() {
        setup:
        SolrContext.setReindexServer(null)

        expect:
        SolrContext.isSingleCoreMode() == true
    }

    def "SolrInputDocument can be created and fields added for indexing"() {
        when:
        SolrInputDocument doc = new SolrInputDocument()
        doc.addField("id", "product_1")
        doc.addField("name", "Test Product")
        doc.addField("price", 29.99)

        then:
        doc.getFieldValue("id") == "product_1"
        doc.getFieldValue("name") == "Test Product"
        doc.getFieldValue("price") == 29.99
    }

    def "SolrQuery can be constructed with search parameters"() {
        when:
        SolrQuery query = new SolrQuery("*:*")
        query.setRows(10)
        query.setStart(0)
        query.addFilterQuery("category:electronics")

        then:
        query.getQuery() == "*:*"
        query.getRows() == 10
        query.getStart() == 0
        query.getFilterQueries().contains("category:electronics")
    }

    def "HttpSolrClient should use builder pattern"() {
        when:
        HttpSolrClient client = new HttpSolrClient.Builder("http://localhost:8983/solr").build()

        then:
        client != null
        client instanceof SolrClient

        cleanup:
        client.close()
    }
}
