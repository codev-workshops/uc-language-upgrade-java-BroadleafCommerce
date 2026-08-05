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
package org.broadleafcommerce.core.search.service.solr;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.locale.domain.Locale;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.core.catalog.dao.ProductDao;
import org.broadleafcommerce.core.catalog.dao.SkuDao;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.search.dao.FieldDao;
import org.broadleafcommerce.core.search.dao.SearchFacetDao;
import org.broadleafcommerce.core.search.domain.CategorySearchFacet;
import org.broadleafcommerce.core.search.domain.Field;
import org.broadleafcommerce.core.search.domain.FieldEntity;
import org.broadleafcommerce.core.search.domain.SearchCriteria;
import org.broadleafcommerce.core.search.domain.SearchFacet;
import org.broadleafcommerce.core.search.domain.SearchFacetDTO;
import org.broadleafcommerce.core.search.domain.SearchFacetRange;
import org.broadleafcommerce.core.search.domain.SearchResult;
import org.broadleafcommerce.core.search.domain.solr.FieldType;
import org.broadleafcommerce.core.search.service.SearchService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Resource;

/**
 * An implementation of SearchService that uses Solr.
 * 
 * Note that prior to 2.2.0, this class used to contain all of the logic for interaction with Solr. Since 2.2.0, this class
 * has been refactored and parts of it have been split into the other classes you can find in this package.
 * 
 * @author Andre Azzolini (apazzolini)
 */
public class SolrSearchServiceImpl implements SearchService, InitializingBean, DisposableBean {
    private static final Log LOG = LogFactory.getLog(SolrSearchServiceImpl.class);

    @Value("${solr.index.use.sku}")
    protected boolean useSku;

    //This is the name of the config that Zookeeper has associated with Solr configs
    @Value("${solr.cloud.configName}")
    protected String solrCloudConfigName = "blc";

    //This is the default number of shards that should be created if a SolrCloud collection is created via API
    @Value("${solr.cloud.defaultNumShards}")
    protected int solrCloudNumShards = 2;

    @Resource(name = "blProductDao")
    protected ProductDao productDao;

    @Resource(name = "blSkuDao")
    protected SkuDao skuDao;

    @Resource(name = "blFieldDao")
    protected FieldDao fieldDao;

    @Resource(name = "blSearchFacetDao")
    protected SearchFacetDao searchFacetDao;

    @Resource(name = "blSolrHelperService")
    protected SolrHelperService shs;

    @Resource(name = "blSolrIndexService")
    protected SolrIndexService solrIndexService;

    @Resource(name = "blSolrSearchServiceExtensionManager")
    protected SolrSearchServiceExtensionManager extensionManager;

    public SolrSearchServiceImpl(SolrClient solrServer) {
        SolrContext.setPrimaryServer(solrServer);
    }

    public SolrSearchServiceImpl(SolrClient solrServer, SolrClient reindexServer) {
        SolrContext.setPrimaryServer(solrServer);
        SolrContext.setReindexServer(reindexServer);
    }

    public SolrSearchServiceImpl(SolrClient solrServer, SolrClient reindexServer, SolrClient adminServer) {
        SolrContext.setPrimaryServer(solrServer);
        SolrContext.setReindexServer(reindexServer);
        SolrContext.setAdminServer(adminServer);
    }

    @Override
    public void rebuildIndex() throws ServiceException, IOException {
        solrIndexService.rebuildIndex();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (SolrContext.isSolrCloudMode()) {
            //We want to use the Solr APIs to make sure the correct collections are set up.

            CloudSolrClient primary = (CloudSolrClient) SolrContext.getServer();
            CloudSolrClient reindex = (CloudSolrClient) SolrContext.getReindexServer();
            if (primary == null || reindex == null) {
                throw new IllegalStateException("The primary and reindex CloudSolrClients must not be null. Check "
                        + "your configuration and ensure that you are passing a different instance for each to the "
                        + "constructor of "
                        + this.getClass().getName()
                        + " and ensure that each has a null (empty)"
                        + " defaultCollection property, or ensure that defaultCollection is unique between"
                        + " the two instances. All other things, like Zookeeper addresses should be the same.");
            }

            if (primary == reindex) {
                //These are the same object instances.  They should be separate instances, with generally 
                //the same configuration, except for the defaultCollection name.
                throw new IllegalStateException("The primary and reindex CloudSolrClients must be different instances "
                        + "and their defaultCollection property must be unique or null.  All other things like the "
                        + "Zookeeper addresses should be the same.");
            }
            
            //Set the default collection if it's null
            if (StringUtils.isEmpty(primary.getDefaultCollection())) {
                primary.setDefaultCollection(SolrContext.PRIMARY);
            }

            //Set the default collection if it's null
            if (StringUtils.isEmpty(reindex.getDefaultCollection())) {
                reindex.setDefaultCollection(SolrContext.REINDEX);
            }

            if (primary.getDefaultCollection().equals(reindex.getDefaultCollection())) {
                throw new IllegalStateException("The primary and reindex CloudSolrClients must have a null (empty) or "
                        + "unique defaultCollection property.  All other things like the "
                        + "Zookeeper addresses should be the same.");
            }

            primary.connect(); //This is required to ensure no NPE!

            createCollectionIfNecessary(primary, primary.getDefaultCollection());
            createCollectionIfNecessary(primary, reindex.getDefaultCollection());
        }
    }

    /**
     * Ensures that an alias with the given name resolves to an existing collection, creating the backing collection
     * and the alias when they do not exist yet.
     */
    protected void createCollectionIfNecessary(CloudSolrClient client, String aliasName) throws Exception {
        Set<String> collectionNames = client.getClusterState().getCollectionsMap().keySet();
        List<String> aliasedCollections = client.getClusterStateProvider().resolveAlias(aliasName);

        if (aliasedCollections.isEmpty() || aliasedCollections.get(0).equals(aliasName)) {
            //No alias exists yet, create a brand new collection and point the alias at it
            String collectionName = generateCollectionName(collectionNames);
            CollectionAdminRequest.createCollection(collectionName, solrCloudConfigName, solrCloudNumShards, 1)
                    .process(client);
            CollectionAdminRequest.createAlias(aliasName, collectionName).process(client);
        } else {
            //Aliases can be mapped to collections that don't exist.... Make sure the collection exists
            String collectionName = aliasedCollections.get(0);
            if (!collectionNames.contains(collectionName)) {
                CollectionAdminRequest.createCollection(collectionName, solrCloudConfigName, solrCloudNumShards, 1)
                        .process(client);
            }
        }
    }

    protected String generateCollectionName(Set<String> existingCollectionNames) {
        for (int i = 0; i < 1000; i++) {
            String collectionName = "blcCollection" + i;
            if (!existingCollectionNames.contains(collectionName)) {
                return collectionName;
            }
        }
        throw new IllegalStateException("Unable to generate a unique Solr collection name");
    }

    @Override
    public void destroy() throws Exception {
        //Make sure we shut down each of the Solr client references
        try {
            if (SolrContext.getServer() != null) {
                SolrContext.getServer().close();
            }
        } catch (Exception e) {
            LOG.error("Error shutting down primary Solr client.", e);
        }

        try {
            if (SolrContext.getReindexServer() != null
                    && SolrContext.getReindexServer() != SolrContext.getServer()) {
                SolrContext.getReindexServer().close();
            }
        } catch (Exception e) {
            LOG.error("Error shutting down reindex Solr client.", e);
        }

        try {
            if (SolrContext.getAdminServer() != null
                    && SolrContext.getAdminServer() != SolrContext.getServer()
                    && SolrContext.getAdminServer() != SolrContext.getReindexServer()) {
                SolrContext.getAdminServer().close();
            }
        } catch (Exception e) {
            LOG.error("Error shutting down admin Solr client.", e);
        }
    }

    @Override
    public SearchResult findExplicitSearchResultsByCategory(Category category, SearchCriteria searchCriteria) throws ServiceException {
        List<SearchFacetDTO> facets = getCategoryFacets(category);
        String query = shs.getExplicitCategoryFieldName() + ":\"" + shs.getCategoryId(category) + "\"";
        return findSearchResults("*:*", facets, searchCriteria, shs.getCategorySortFieldName(category) + " asc", query);
    }

    @Override
    public SearchResult findSearchResultsByCategory(Category category, SearchCriteria searchCriteria) throws ServiceException {
        List<SearchFacetDTO> facets = getCategoryFacets(category);
        String query = shs.getCategoryFieldName() + ":\"" + shs.getCategoryId(category) + "\"";
        return findSearchResults("*:*", facets, searchCriteria, shs.getCategorySortFieldName(category) + " asc", query);
    }

    @Override
    public SearchResult findSearchResultsByQuery(String query, SearchCriteria searchCriteria) throws ServiceException {
        List<SearchFacetDTO> facets = getSearchFacets();
        query = "(" + sanitizeQuery(query) + ")";
        return findSearchResults(query, facets, searchCriteria, null);
    }

    @Override
    public SearchResult findSearchResultsByCategoryAndQuery(Category category, String query, SearchCriteria searchCriteria) throws ServiceException {
        List<SearchFacetDTO> facets = getSearchFacets();

        String catFq = shs.getCategoryFieldName() + ":\"" + shs.getCategoryId(category) + "\"";
        query = "(" + sanitizeQuery(query) + ")";
        
        return findSearchResults(query, facets, searchCriteria, null, catFq);
    }

    public String getLocalePrefix() {
        if (BroadleafRequestContext.getBroadleafRequestContext() != null) {
            Locale locale = BroadleafRequestContext.getBroadleafRequestContext().getLocale();
            if (locale != null) {
                return locale.getLocaleCode() + "_";
            }
        }
        return "";
    }

    protected String buildQueryFieldsString() {
        StringBuilder queryBuilder = new StringBuilder();
        List<Field> fields = null;
        if (useSku) {
            fields = fieldDao.readAllSkuFields();
        } else {
            fields = fieldDao.readAllProductFields();
        }

        for (Field currentField : fields) {
            if (currentField.getSearchable()) {
                appendFieldToQuery(queryBuilder, currentField);
            }
        }
        return queryBuilder.toString();
    }

    protected void appendFieldToQuery(StringBuilder queryBuilder, Field currentField) {
        List<FieldType> searchableFieldTypes = shs.getSearchableFieldTypes(currentField);
        for (FieldType currentType : searchableFieldTypes) {
            queryBuilder.append(shs.getPropertyNameForFieldSearchable(currentField, currentType)).append(" ");
        }
    }

    /**
     * @deprecated in favor of the other findSearchResults() method
     */
    @Deprecated
    protected SearchResult findSearchResults(String qualifiedSolrQuery, List<SearchFacetDTO> facets,
            SearchCriteria searchCriteria, String defaultSort) throws ServiceException {
        return findSearchResults(qualifiedSolrQuery, facets, searchCriteria, defaultSort, (String[]) null);
    }

    /**
     * Given a qualified solr query string (such as "category:2002"), actually performs a solr search. It will
     * take into considering the search criteria to build out facets / pagination / sorting.
     *
     * @param qualifiedSolrQuery
     * @param facets
     * @param searchCriteria
     * @return the ProductSearchResult of the search
     * @throws ServiceException
     */
    protected SearchResult findSearchResults(String qualifiedSolrQuery, List<SearchFacetDTO> facets, SearchCriteria searchCriteria, String defaultSort, String... filterQueries) throws ServiceException {
        Map<String, SearchFacetDTO> namedFacetMap = getNamedFacetMap(facets, searchCriteria);

        // Build the basic query
        // Solr queries with a 'start' parameter cannot be a negative number
        int start = (searchCriteria.getPage() <= 0) ? 0 : (searchCriteria.getPage() - 1);
        SolrQuery solrQuery = new SolrQuery()
                .setQuery(qualifiedSolrQuery)
                .setRows(searchCriteria.getPageSize())
                .setStart((start) * searchCriteria.getPageSize());

        //This is for SolrCloud.  We assume that we are always searching against a collection aliased as "PRIMARY"
        solrQuery.setParam("collection", SolrContext.PRIMARY); //This should be ignored if not using SolrCloud

        if (useSku) {
            solrQuery.setFields(shs.getSkuIdFieldName());
        } else {
            solrQuery.setFields(shs.getProductIdFieldName());
        }
        if (filterQueries != null) {
            solrQuery.setFilterQueries(filterQueries);
        }
        solrQuery.addFilterQuery(shs.getNamespaceFieldName() + ":(\"" + shs.getCurrentNamespace() + "\")");
        solrQuery.set("defType", "edismax");
        solrQuery.set("qf", buildQueryFieldsString());

        // Attach additional restrictions
        attachSortClause(solrQuery, searchCriteria, defaultSort);
        attachActiveFacetFilters(solrQuery, namedFacetMap, searchCriteria);
        attachFacets(solrQuery, namedFacetMap);
        
        modifySolrQuery(solrQuery, qualifiedSolrQuery, facets, searchCriteria, defaultSort);
        
        extensionManager.getProxy().modifySolrQuery(solrQuery, qualifiedSolrQuery, facets,
                searchCriteria, defaultSort);

        if (LOG.isTraceEnabled()) {
            try {
                LOG.trace(URLDecoder.decode(solrQuery.toString(), "UTF-8"));
            } catch (Exception e) {
                LOG.trace("Couldn't UTF-8 URL Decode: " + solrQuery.toString());
            }
        }

        // Query solr
        QueryResponse response;
        List<SolrDocument> responseDocuments;
        int numResults = 0;
        try {
            response = SolrContext.getServer().query(solrQuery, getSolrQueryMethod());
            responseDocuments = getResponseDocuments(response);
            numResults = (int) response.getResults().getNumFound();

            if (LOG.isTraceEnabled()) {
                LOG.trace(response.toString());

                for (SolrDocument doc : responseDocuments) {
                    LOG.trace(doc);
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new ServiceException("Could not perform search", e);
        }

        // Get the facets
        setFacetResults(namedFacetMap, response);
        sortFacetResults(namedFacetMap);

        SearchResult result = new SearchResult();
        result.setFacets(facets);
        setPagingAttributes(result, numResults, searchCriteria);

        if (useSku) {
            List<Sku> skus = getSkus(responseDocuments);
            result.setSkus(skus);
        } else {
            // Get the products
            List<Product> products = getProducts(responseDocuments);
            result.setProducts(products);
        }

        return result;
    }

    /**
     * Provides a hook point for implementations to modify all SolrQueries before they're executed.
     * Modules should leverage the extension manager method of the same name,
     * {@link SolrSearchServiceExtensionHandler#modifySolrQuery(SolrQuery, String, List, SearchCriteria, String)}
     * 
     * @param query
     * @param qualifiedSolrQuery
     * @param facets
     * @param searchCriteria
     * @param defaultSort
     */
    protected void modifySolrQuery(SolrQuery query, String qualifiedSolrQuery,
            List<SearchFacetDTO> facets, SearchCriteria searchCriteria, String defaultSort) {
    }
    
    protected List<SolrDocument> getResponseDocuments(QueryResponse response) {
        return shs.getResponseDocuments(response);
    }

    @Override
    public List<SearchFacetDTO> getSearchFacets() {
        if (useSku) {
            return buildSearchFacetDTOs(searchFacetDao.readAllSearchFacets(FieldEntity.SKU));
        }
        return buildSearchFacetDTOs(searchFacetDao.readAllSearchFacets(FieldEntity.PRODUCT));
    }

    @Override
    public List<SearchFacetDTO> getCategoryFacets(Category category) {
        List<CategorySearchFacet> categorySearchFacets = category.getCumulativeSearchFacets();

        List<SearchFacet> searchFacets = new ArrayList<SearchFacet>();
        for (CategorySearchFacet categorySearchFacet : categorySearchFacets) {
            searchFacets.add(categorySearchFacet.getSearchFacet());
        }

        return buildSearchFacetDTOs(searchFacets);
    }

    /**
     * Sets up the sorting criteria. This will support sorting by multiple fields at a time
     * 
     * @param query
     * @param searchCriteria
     */
    protected void attachSortClause(SolrQuery query, SearchCriteria searchCriteria, String defaultSort) {
        List<Field> fields = null;
        if (useSku) {
            fields = fieldDao.readAllSkuFields();
        } else {
            fields = fieldDao.readAllProductFields();
        }
        shs.attachSortClause(query, searchCriteria, defaultSort, fields);
    }

    /**
     * Restricts the query by adding active facet filters.
     * 
     * @param query
     * @param namedFacetMap
     * @param searchCriteria
     */
    protected void attachActiveFacetFilters(SolrQuery query, Map<String, SearchFacetDTO> namedFacetMap,
            SearchCriteria searchCriteria) {
        shs.attachActiveFacetFilters(query, namedFacetMap, searchCriteria);
    }
    
    /**
     * Scrubs a facet value string for all Solr special characters, automatically adding escape characters
     * 
     * @param facetValue The raw facet value
     * @return The facet value with all special characters properly escaped, safe to be used in construction of a Solr query
     */
    protected String scrubFacetValue(String facetValue) {
        return shs.scrubFacetValue(facetValue);
    }

    /**
     * Notifies solr about which facets you want it to determine results and counts for
     * 
     * @param query
     * @param namedFacetMap
     */
    protected void attachFacets(SolrQuery query, Map<String, SearchFacetDTO> namedFacetMap) {
        shs.attachFacets(query, namedFacetMap);
    }

    /**
     * Builds out the DTOs for facet results from the search. This will then be used by the view layer to
     * display which values are available given the current constraints as well as the count of the values.
     * 
     * @param namedFacetMap
     * @param response
     */
    protected void setFacetResults(Map<String, SearchFacetDTO> namedFacetMap, QueryResponse response) {
        shs.setFacetResults(namedFacetMap, response);
    }

    /**
     * Invoked to sort the facet results. This method will use the natural sorting of the value attribute of the
     * facet (or, if value is null, the minValue of the facet result). Override this method to customize facet
     * sorting for your given needs.
     * 
     * @param namedFacetMap
     */
    protected void sortFacetResults(Map<String, SearchFacetDTO> namedFacetMap) {
        shs.sortFacetResults(namedFacetMap);
    }

    /**
     * Sets the total results, the current page, and the page size on the ProductSearchResult. Total results comes
     * from solr, while page and page size are duplicates of the searchCriteria conditions for ease of use.
     * 
     * @param result
     * @param response
     * @param searchCriteria
     */
    public void setPagingAttributes(SearchResult result, int numResults, SearchCriteria searchCriteria) {
        result.setTotalResults(numResults);
        result.setPage(searchCriteria.getPage());
        result.setPageSize(searchCriteria.getPageSize());
    }

    /**
     * Given a list of product IDs from solr, this method will look up the IDs via the productDao and build out
     * actual Product instances. It will return a Products that is sorted by the order of the IDs in the passed
     * in list.
     * 
     * @param response
     * @return the actual Product instances as a result of the search
     */
    protected List<Product> getProducts(List<SolrDocument> responseDocuments) {
        final List<Long> productIds = new ArrayList<Long>();
        for (SolrDocument doc : responseDocuments) {
            productIds.add((Long) doc.getFieldValue(shs.getProductIdFieldName()));
        }

        List<Product> products = productDao.readProductsByIds(productIds);

        extensionManager.getProxy().batchFetchCatalogData(products);

        // We have to sort the products list by the order of the productIds list to maintain sortability in the UI
        if (products != null) {
            Collections.sort(products, new Comparator<Product>() {
                @Override
                public int compare(Product o1, Product o2) {
                    Long o1id = shs.getProductId(o1);
                    Long o2id = shs.getProductId(o2);
                    return new Integer(productIds.indexOf(o1id)).compareTo(productIds.indexOf(o2id));
                }
            });
        }

        return products;
    }

    /**
     * Given a list of Sku IDs from solr, this method will look up the IDs via the skuDao and build out
     * actual Sku instances. It will return a Sku list that is sorted by the order of the IDs in the passed
     * in list.
     * 
     * @param response
     * @return the actual Sku instances as a result of the search
     */
    protected List<Sku> getSkus(List<SolrDocument> responseDocuments) {
        final List<Long> skuIds = new ArrayList<Long>();
        for (SolrDocument doc : responseDocuments) {
            skuIds.add((Long) doc.getFieldValue(shs.getSkuIdFieldName()));
        }

        List<Sku> skus = skuDao.readSkusByIds(skuIds);

        // We have to sort the skus list by the order of the skuIds list to maintain sortability in the UI
        if (skus != null) {
            Collections.sort(skus, new Comparator<Sku>() {
                @Override
                public int compare(Sku o1, Sku o2) {
                    return new Integer(skuIds.indexOf(o1.getId())).compareTo(skuIds.indexOf(o2.getId()));
                }
            });
        }

        return skus;
    }

    /**
     * Create the wrapper DTO around the SearchFacet
     * 
     * @param searchFacets
     * @return the wrapper DTO
     */
    protected List<SearchFacetDTO> buildSearchFacetDTOs(List<SearchFacet> searchFacets) {
        return shs.buildSearchFacetDTOs(searchFacets);
    }

    /**
     * Checks to see if the requiredFacets condition for a given facet is met.
     * 
     * @param facet
     * @param request
     * @return whether or not the facet parameter is available 
     */
    protected boolean facetIsAvailable(SearchFacet facet, Map<String, String[]> params) {
        return shs.isFacetAvailable(facet, params);
    }

    /**
     * Perform any necessary query sanitation here. For example, we disallow open and close parentheses, colons, and we also
     * ensure that quotes are actual quotes (") and not the URL encoding (&quot;) so that Solr is able to properly handle
     * the user's intent.
     * 
     * @param query
     * @return the sanitized query
     */
    protected String sanitizeQuery(String query) {
        return shs.sanitizeQuery(query);
    }

    /**
     * Returns a fully composed solr field string. Given indexField = a, tag = ex, and a non-null range,
     * would produce the following String: {!tag=a frange incl=false l=minVal u=maxVal}a
     */
    protected String getSolrTaggedFieldString(String indexField, String tag, SearchFacetRange range) {
        return shs.getSolrTaggedFieldString(indexField, tag, range);
    }

    /**
     * Returns a solr field tag. Given indexField = a, tag = tag, would produce the following String:
     * {!tag=a}. if range is not null it will produce {!tag=a frange incl=false l=minVal u=maxVal} 
     */
    protected String getSolrFieldTag(String tagField, String tag, SearchFacetRange range) {
        return shs.getSolrFieldTag(tagField, tag, range);
    }

    protected String getSolrRangeString(String fieldName, BigDecimal minValue, BigDecimal maxValue) {
        return shs.getSolrRangeString(fieldName, minValue, maxValue);
    }

    /**
     * @param minValue
     * @param maxValue
     * @return a string representing a call to the frange solr function. it is not inclusive of lower limit, inclusive of upper limit
     */
    protected String getSolrRangeFunctionString(BigDecimal minValue, BigDecimal maxValue) {
        return shs.getSolrRangeFunctionString(minValue, maxValue);
    }

    /**
     * @param facets
     * @param searchCriteria
     * @return a map of fully qualified solr index field key to the searchFacetDTO object
     */
    protected Map<String, SearchFacetDTO> getNamedFacetMap(List<SearchFacetDTO> facets,
            final SearchCriteria searchCriteria) {
        return shs.getNamedFacetMap(facets, searchCriteria);
    }

    /**
     * @param searchCriteria
     * @return a map of abbreviated key to fully qualified solr index field key for all product fields
     */
    protected Map<String, String> getSolrFieldKeyMap(SearchCriteria searchCriteria) {
        List<Field> fields = null;
        if (useSku) {
            fields = fieldDao.readAllSkuFields();
        } else {
            fields = fieldDao.readAllProductFields();
        }
        return shs.getSolrFieldKeyMap(searchCriteria, fields);
    }

    /**
     * Allows the user to choose the query method to use.  POST allows for longer, more complex queries with 
     * a higher number of facets.
     * 
     * Default value is POST.  Implementors can override this to use GET if they wish.
     * 
     * @return
     */
    protected METHOD getSolrQueryMethod() {
        return METHOD.POST;
    }
}
