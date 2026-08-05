/*
 * #%L
 * BroadleafCommerce Framework
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
package org.broadleafcommerce.core.search.service.solr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.junit.After;
import org.junit.Test;

/**
 * Verifies that {@link SolrContext} holds SolrJ 9 {@link SolrClient} instances, which replaced the removed
 * SolrServer/EmbeddedSolrServer types.
 */
public class SolrContextTest {

    @After
    public void clearContext() {
        SolrContext.setPrimaryServer(null);
        SolrContext.setReindexServer(null);
        SolrContext.setAdminServer(null);
    }

    @Test
    public void testSingleCoreModeUsesPrimaryClientForReindexAndAdmin() {
        SolrClient primary = new Http2SolrClient.Builder("http://localhost:8983/solr/primary").build();
        SolrContext.setPrimaryServer(primary);

        assertTrue(SolrContext.isSingleCoreMode());
        assertSame(primary, SolrContext.getServer());
        assertSame(primary, SolrContext.getReindexServer());
        assertSame(primary, SolrContext.getAdminServer());
        assertFalse(SolrContext.isSolrCloudMode());
    }

    @Test
    public void testSeparateReindexAndAdminClients() {
        SolrClient primary = new Http2SolrClient.Builder("http://localhost:8983/solr/primary").build();
        SolrClient reindex = new Http2SolrClient.Builder("http://localhost:8983/solr/reindex").build();
        SolrClient admin = new Http2SolrClient.Builder("http://localhost:8983/solr").build();
        SolrContext.setPrimaryServer(primary);
        SolrContext.setReindexServer(reindex);
        SolrContext.setAdminServer(admin);

        assertFalse(SolrContext.isSingleCoreMode());
        assertSame(reindex, SolrContext.getReindexServer());
        assertSame(admin, SolrContext.getAdminServer());
    }
}
