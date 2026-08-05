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
package org.broadleafcommerce.core.catalog.dao;

import org.broadleafcommerce.common.persistence.EntityConfiguration;
import org.broadleafcommerce.common.sandbox.SandBoxHelper;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductOption;
import org.broadleafcommerce.core.catalog.domain.ProductOptionImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValue;
import org.broadleafcommerce.core.catalog.domain.ProductOptionValueImpl;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXref;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXrefImpl;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuProductOptionValueXrefImpl;
import org.broadleafcommerce.core.catalog.domain.dto.AssignedProductOptionDTO;
import org.hibernate.jpa.HibernateHints;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository("blProductOptionDao")
public class ProductOptionDaoImpl implements ProductOptionDao {
    
    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    @Resource(name="blEntityConfiguration")
    protected EntityConfiguration entityConfiguration;

    @Resource(name="blSandBoxHelper")
    protected SandBoxHelper sandBoxHelper;
    
    @Override
    public List<ProductOption> readAllProductOptions() {
        TypedQuery<ProductOption> query = em.createNamedQuery("BC_READ_ALL_PRODUCT_OPTIONS", ProductOption.class);
        return query.getResultList();
    }
    
    public ProductOption saveProductOption(ProductOption option) {
        return em.merge(option);
    }

    @Override
    public ProductOption readProductOptionById(Long id) {
        return em.find(ProductOptionImpl.class, id);
    }

    @Override
    public ProductOptionValue readProductOptionValueById(Long id) {
        return em.find(ProductOptionValueImpl.class, id);
    }

    @Override
    public List<AssignedProductOptionDTO> findAssignedProductOptionsByProductId(Long productId) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
        Root<SkuProductOptionValueXrefImpl> xref = criteria.from(SkuProductOptionValueXrefImpl.class);
        Join<Object, Object> sku = xref.join("sku");
        Join<Object, Object> product = sku.join("product");
        Join<Object, Object> productOptionValue = xref.join("productOptionValue");
        Join<Object, Object> productOption = productOptionValue.join("productOption");

        criteria.multiselect(
                product.get("id"),
                productOption.get("attributeName"),
                productOptionValue,
                sku)
            .distinct(true)
            .where(builder.equal(product.get("id"), productId))
            .orderBy(builder.asc(productOption.get("attributeName")));

        List<AssignedProductOptionDTO> results = new ArrayList<AssignedProductOptionDTO>();
        for (Tuple tuple : em.createQuery(criteria).getResultList()) {
            AssignedProductOptionDTO dto = new AssignedProductOptionDTO();
            dto.setProductId((Long) tuple.get(0));
            dto.setProductOptionAttrName((String) tuple.get(1));
            dto.setProductOptionValue((ProductOptionValue) tuple.get(2));
            dto.setSku((Sku) tuple.get(3));
            if (dto.getSku().isActive()) {
                results.add(dto);
            }
        }
        return results;
    }

    @Override
    public List<AssignedProductOptionDTO> findAssignedProductOptionsByProduct(Product product) {
        return findAssignedProductOptionsByProductId(product.getId());
    }

    @Override
    public Long countAllowedValuesForProductOptionById(Long productOptionId) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<ProductOptionValueImpl> root = criteria.from(ProductOptionValueImpl.class);
        criteria.select(builder.count(root));

        List<Predicate> restrictions = new ArrayList<Predicate>();
        List<Long> mergedIds = sandBoxHelper.mergeCloneIds(ProductOptionImpl.class, productOptionId);
        restrictions.add(root.get("productOption").in(mergedIds));
        criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));

        TypedQuery<Long> query = em.createQuery(criteria);
        return query.getSingleResult();
    }

    @Override
    public Long countProductsUsingProductOptionById(Long productOptionId) {
        TypedQuery<Long> query = getProductIdsUsingProductOptionByIdQuery(productOptionId, true);
        query.setHint(HibernateHints.HINT_CACHEABLE, true);
        return query.getSingleResult();
    }

    @Override
    public List<Long> findProductIdsUsingProductOptionById(Long productOptionId, int start, int pageSize) {
        TypedQuery<Long> query = getProductIdsUsingProductOptionByIdQuery(productOptionId, false);
        query.setFirstResult(start);
        query.setMaxResults(pageSize);
        query.setHint(HibernateHints.HINT_CACHEABLE, true);
        return query.getResultList();

    }

    private TypedQuery<Long> getProductIdsUsingProductOptionByIdQuery(Long productOptionId, boolean count) {
        // Set up the criteria query that specifies we want to return Products
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);

        // The root of our search is ProductOptionXref
        Root<ProductOptionXrefImpl> productOptionXref = criteria.from(ProductOptionXrefImpl.class);
        Join<ProductOptionXref, Product> product = productOptionXref.join("product");
        Join<ProductOptionXref, ProductOption> productOption = productOptionXref.join("productOption");

        if (count) {
            criteria.select(builder.count(product));
        } else {
            // Product IDs are what we want back
            criteria.select(product.get("id").as(Long.class));
        }
        criteria.distinct(true);

        List<Predicate> restrictions = new ArrayList<Predicate>();
        restrictions.add(productOption.get("id").in(sandBoxHelper.mergeCloneIds(ProductOptionImpl.class, productOptionId)));

        // Execute the query with the restrictions
        criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));
        return em.createQuery(criteria);
    }

}
