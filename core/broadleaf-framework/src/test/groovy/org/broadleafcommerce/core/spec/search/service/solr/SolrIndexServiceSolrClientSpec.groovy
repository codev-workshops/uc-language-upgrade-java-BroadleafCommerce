/*
 * #%L
 * BroadleafCommerce Framework
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
package org.broadleafcommerce.core.spec.search.service.solr

import org.apache.solr.client.solrj.SolrClient
import org.broadleafcommerce.core.search.service.solr.SolrHelperService
import org.broadleafcommerce.core.search.service.solr.SolrHelperServiceImpl
import org.broadleafcommerce.core.search.service.solr.SolrIndexServiceImpl

import spock.lang.Specification

/**
 * Unit tests for the SolrJ 4.x -&gt; 8.11 client rewrite as it surfaces in the index service. The
 * SolrJ 8 {@code SolrClient.commit(...)} / {@code optimize()} API replaced the old {@code SolrServer}
 * equivalents; these tests pin the commit-flag translation (note the parameter re-ordering) and the
 * optimize delegation against a mocked {@code SolrClient}. No real Solr instance is started.
 */
class SolrIndexServiceSolrClientSpec extends Specification {

    SolrIndexServiceImpl indexService = new SolrIndexServiceImpl()
    SolrClient mockClient = Mock()

    def "A forced commit maps (softCommit, waitSearcher, waitFlush) onto SolrClient.commit(waitFlush, waitSearcher, softCommit)"() {
        when:
        indexService.commit(mockClient, true, true, false)

        then:
        1 * mockClient.commit(false, true, true)
    }

    def "The single-arg commit is a no-op when the commit flag is disabled"() {
        given:
        indexService.commit = false

        when:
        indexService.commit(mockClient)

        then:
        0 * mockClient.commit(_, _, _)
    }

    def "The single-arg commit honors the configured commit flags when enabled"() {
        given:
        indexService.commit = true
        indexService.softCommit = false
        indexService.waitSearcher = true
        indexService.waitFlush = true

        when:
        indexService.commit(mockClient)

        then:
        1 * mockClient.commit(true, true, false)
    }

    def "optimizeIndex delegates to the SolrHelperService with the same client"() {
        given:
        SolrHelperService shs = Mock()
        indexService.shs = shs

        when:
        indexService.optimizeIndex(mockClient)

        then:
        1 * shs.optimizeIndex(mockClient)
    }

    def "SolrHelperServiceImpl.optimizeIndex optimizes the provided SolrClient"() {
        given:
        SolrHelperServiceImpl helper = new SolrHelperServiceImpl()

        when:
        helper.optimizeIndex(mockClient)

        then:
        1 * mockClient.optimize()
    }
}
