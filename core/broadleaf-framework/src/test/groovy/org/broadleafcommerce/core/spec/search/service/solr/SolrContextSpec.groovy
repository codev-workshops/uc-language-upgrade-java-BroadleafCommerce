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
import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.broadleafcommerce.core.search.service.solr.SolrContext

import spock.lang.Specification

/**
 * Unit tests for the SolrJ 4.x -&gt; 8.11 client rewrite of {@link SolrContext}. The legacy
 * {@code SolrServer}/{@code CloudSolrServer} types were replaced with {@code SolrClient}/{@code CloudSolrClient};
 * these tests pin the collection-naming, single/dual-core and SolrCloud-detection behavior that the rewrite
 * must keep equivalent. No real Solr instance is started; the client is mocked.
 */
class SolrContextSpec extends Specification {

    def cleanup() {
        // SolrContext stores the clients in static fields, so reset between features.
        SolrContext.setPrimaryServer(null)
        SolrContext.setReindexServer(null)
        SolrContext.setAdminServer(null)
    }

    def "A blank-collection CloudSolrClient primary is assigned the PRIMARY collection"() {
        given:
        CloudSolrClient primary = Mock()
        primary.getDefaultCollection() >> null

        when:
        SolrContext.setPrimaryServer(primary)

        then:
        1 * primary.setDefaultCollection(SolrContext.PRIMARY)
    }

    def "A blank-collection CloudSolrClient reindex is assigned the REINDEX collection"() {
        given:
        CloudSolrClient reindex = Mock()
        reindex.getDefaultCollection() >> ""

        when:
        SolrContext.setReindexServer(reindex)

        then:
        1 * reindex.setDefaultCollection(SolrContext.REINDEX)
    }

    def "Using the same CloudSolrClient instance for primary and reindex is rejected"() {
        given:
        CloudSolrClient cloud = Mock()
        cloud.getDefaultCollection() >> "primary"

        when:
        SolrContext.setReindexServer(cloud)
        SolrContext.setPrimaryServer(cloud)

        then:
        thrown(IllegalArgumentException)
    }

    def "Primary and reindex CloudSolrClients sharing a default collection are rejected"() {
        given:
        CloudSolrClient primary = Mock()
        CloudSolrClient reindex = Mock()
        primary.getDefaultCollection() >> "dup"
        reindex.getDefaultCollection() >> "dup"

        when:
        SolrContext.setReindexServer(reindex)
        SolrContext.setPrimaryServer(primary)

        then:
        thrown(IllegalStateException)
    }

    def "isSingleCoreMode is true when no reindex server is set"() {
        given:
        SolrClient primary = Mock()

        when:
        SolrContext.setPrimaryServer(primary)

        then:
        SolrContext.isSingleCoreMode()
        SolrContext.getReindexServer() == primary
    }

    def "isSingleCoreMode is false and getReindexServer returns the reindex server in dual-core mode"() {
        given:
        SolrClient primary = Mock()
        SolrClient reindex = Mock()

        when:
        SolrContext.setPrimaryServer(primary)
        SolrContext.setReindexServer(reindex)

        then:
        !SolrContext.isSingleCoreMode()
        SolrContext.getReindexServer() == reindex
    }

    def "getAdminServer falls back to the primary server when no admin server is configured"() {
        given:
        SolrClient primary = Mock()

        when:
        SolrContext.setPrimaryServer(primary)

        then:
        SolrContext.getAdminServer() == primary
    }

    def "getAdminServer returns the configured admin server when set"() {
        given:
        SolrClient primary = Mock()
        SolrClient admin = Mock()

        when:
        SolrContext.setPrimaryServer(primary)
        SolrContext.setAdminServer(admin)

        then:
        SolrContext.getAdminServer() == admin
    }

    def "isSolrCloudMode is false for a non-cloud primary server"() {
        given:
        SolrClient nonCloud = Mock()

        when:
        SolrContext.setPrimaryServer(nonCloud)

        then:
        !SolrContext.isSolrCloudMode()
    }

    def "isSolrCloudMode is true for a CloudSolrClient primary server"() {
        given:
        CloudSolrClient cloud = Mock()
        cloud.getDefaultCollection() >> "primary"

        when:
        SolrContext.setPrimaryServer(cloud)

        then:
        SolrContext.isSolrCloudMode()
    }
}
