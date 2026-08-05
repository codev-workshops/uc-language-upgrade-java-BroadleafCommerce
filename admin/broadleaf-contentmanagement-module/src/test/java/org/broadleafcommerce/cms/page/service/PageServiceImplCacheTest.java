/*
 * #%L
 * BroadleafCommerce CMS Module
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
package org.broadleafcommerce.cms.page.service;

import org.broadleafcommerce.common.cache.JCacheUtil;
import org.broadleafcommerce.common.page.dto.PageDTO;
import org.broadleafcommerce.common.cache.StatisticsService;
import org.broadleafcommerce.common.cache.StatisticsServiceImpl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Verifies the JCache based page caching that replaced the Ehcache 2 {@code Element} API.
 */
public class PageServiceImplCacheTest extends TestCase {

    private PageServiceImpl pageService;

    @Override
    protected void setUp() {
        pageService = new PageServiceImpl();
        StatisticsService statisticsService = new StatisticsServiceImpl();
        pageService.statisticsService = statisticsService;
        pageService.pageCache = JCacheUtil.getCache("testPageCache");
        pageService.pageMapCache = JCacheUtil.getCache("testPageMapCache");
        pageService.pageCache.clear();
        pageService.pageMapCache.clear();
    }

    public void testCachedPageListIsReturnedAndEvicted() {
        List<PageDTO> pages = new ArrayList<PageDTO>();
        pages.add(new PageDTO());
        String key = "/test-page-1-2";

        pageService.addPageListToCache(pages, key, "/test-page", 1L, 2L);

        assertEquals(pages, pageService.getPageListFromCache(key));

        pageService.removePageFromCache(pageService.getPageMapCacheKey("/test-page", 1L, 2L));

        assertNull(pageService.getPageListFromCache(key));
    }
}
