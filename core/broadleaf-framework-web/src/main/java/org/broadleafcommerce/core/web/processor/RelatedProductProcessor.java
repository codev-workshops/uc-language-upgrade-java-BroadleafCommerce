/*
 * #%L
 * BroadleafCommerce Framework Web
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
package org.broadleafcommerce.core.web.processor;

import org.apache.commons.collections.CollectionUtils;
import org.broadleafcommerce.common.web.dialect.AbstractModelVariableModifierProcessor;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.PromotableProduct;
import org.broadleafcommerce.core.catalog.domain.RelatedProductDTO;
import org.broadleafcommerce.core.catalog.domain.RelatedProductTypeEnum;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.service.RelatedProductsService;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;


/**
 * A Thymeleaf processor that will find related products and skus.    A product or category id must be specified.    If both are specified, only the productId will be used.  
 *
 * Takes in the following parameters
 * <ul>
 *    <li>productId - productId to find related products.</li>
 *    <li>categoryId - categoryId to find related products.</li>
 *    <li>type - the type of relations to find (e.g. FEATURED (DEFAULT), UPSELL, CROSSSELL).   Implementations may have other specific types of related products.</li>  
 *    <li>cumulativeResults - true (DEFAULT) /false - indicates that the system should add results from the parent categories of the passed in item as well as the current item</li>
 *    <li>qty - if specified, determines the max-number of results that will be returned; otherwise, all results are returned.
 *    <li>productsResultVar - if specified, adds the products to the model keyed by this var.   Otherwise, uses "products" as the model identifier.
 *    <li>relatedProductsResultVar - if specified, adds the RelatedProduct(s) to the model keyed by this var.   Otherwise, uses "relatedProducts" as the model identifier.   
 *    <li>relatedSkusResultVar - if specified, adds the related skus to the model keyed by this var.   Otherwise, uses "relatedSkus" as the model identifier.   
 * </ul>
 * 
 * The output from this operation returns a list of PromotableProducts which represent the following. 
 *      relatedProduct.product 
 *      relatedProduct.promotionMessage.
 *      
 * @author bpolster
 */
public class RelatedProductProcessor extends AbstractModelVariableModifierProcessor {
    
    @Value("${solr.index.use.sku}")
    protected boolean useSku;
    
    @Resource(name = "blRelatedProductsService")
    protected RelatedProductsService relatedProductsService;

    /**
     * Sets the name of this processor to be used in Thymeleaf template
     */
    public RelatedProductProcessor() {
        super(DIALECT_PREFIX, "related_products", 10000);
    }
    

    @Override
    /**
     * Controller method for the processor that readies the service call and adds the results to the model.
     */
    protected void modifyModelAttributes(ITemplateContext context, IProcessableElementTag tag,
                                         IElementTagStructureHandler structureHandler) {
        RelatedProductDTO relatedProductDTO = buildDTO(context, tag);
        List<? extends PromotableProduct> relatedProducts = relatedProductsService.findRelatedProducts(relatedProductDTO);
        if (useSku) {
            addToModel(structureHandler, getRelatedSkusResultVar(tag), getRelatedSkus(relatedProducts, relatedProductDTO.getQuantity()));
        } else {
            addToModel(structureHandler, getRelatedProductsResultVar(tag), relatedProducts);
            addToModel(structureHandler, getProductsResultVar(tag), convertRelatedProductsToProducts(relatedProducts));
            addCollectionToExistingSet(context, structureHandler, "blcAllProducts", buildProductList(relatedProducts));
        }
    }

    protected List<Product> buildProductList(List<? extends PromotableProduct> relatedProducts) {
        List<Product> productList = new ArrayList<Product>();
        if (relatedProducts != null) {
            for (PromotableProduct promProduct : relatedProducts) {
                productList.add(promProduct.getRelatedProduct());
            }
        }
        return productList;
    }
    
    protected List<Sku> getRelatedSkus(List<? extends PromotableProduct> relatedProducts, Integer maxQuantity) {
        List<Sku> relatedSkus = new ArrayList<Sku>();
        if (relatedProducts != null) {
            Integer numSkus = 0;
            for (PromotableProduct promProduct : relatedProducts) {
                Product relatedProduct = promProduct.getRelatedProduct();
                List<Sku> additionalSkus = relatedProduct.getAdditionalSkus();
                if(CollectionUtils.isNotEmpty(additionalSkus)) {
                    for(Sku additionalSku : additionalSkus) {
                        if(numSkus == maxQuantity) {
                            break;
                        }
                        relatedSkus.add(additionalSku);
                        numSkus++;
                        
                    }
                } else {
                    if(numSkus.equals(maxQuantity)) {
                        break;
                    }
                    relatedSkus.add(relatedProduct.getDefaultSku());
                    numSkus++;
                }
            }
        }
        return relatedSkus;
    }
    
    protected List<Product> convertRelatedProductsToProducts(List<? extends PromotableProduct> relatedProducts) {
        List<Product> products = new ArrayList<Product>();
        if (relatedProducts != null) {
            for (PromotableProduct product : relatedProducts) {
                products.add(product.getRelatedProduct());
            }
        }
        return products;        
    }
    
    private String getRelatedProductsResultVar(IProcessableElementTag tag) {
        String resultVar = tag.getAttributeValue("relatedProductsResultVar");       
        if (resultVar == null) {
            resultVar = "relatedProducts";
        }
        return resultVar;
    }
    
    private String getRelatedSkusResultVar(IProcessableElementTag tag) {
        String resultVar = tag.getAttributeValue("relatedSkusResultVar");       
        if (resultVar == null) {
            resultVar = "relatedSkus";
        }
        return resultVar;
    }
    
    private String getProductsResultVar(IProcessableElementTag tag) {
        String resultVar = tag.getAttributeValue("productsResultVar");      
        if (resultVar == null) {
            resultVar = "products";
        }
        return resultVar;
    }

    private RelatedProductDTO buildDTO(ITemplateContext context, IProcessableElementTag tag) {
        RelatedProductDTO relatedProductDTO = new RelatedProductDTO();
        String productIdStr = tag.getAttributeValue("productId"); 
        String categoryIdStr = tag.getAttributeValue("categoryId"); 
        String quantityStr = tag.getAttributeValue("quantity"); 
        String typeStr = tag.getAttributeValue("type"); 
        
        if (productIdStr != null) {
            IStandardExpression expression1 = StandardExpressions.getExpressionParser(context.getConfiguration())
                    .parseExpression(context, productIdStr);
            Object productId = expression1.execute(context);
            if (productId instanceof BigDecimal) {
                productId = new Long(((BigDecimal) productId).toPlainString());
            }
            relatedProductDTO.setProductId((Long) productId);
        }
        
        if (categoryIdStr != null) {
            IStandardExpression expression2 = StandardExpressions.getExpressionParser(context.getConfiguration())
                    .parseExpression(context, categoryIdStr);
            Object categoryId = expression2.execute(context);
            if (categoryId instanceof BigDecimal) {
                categoryId = new Long(((BigDecimal) categoryId).toPlainString());
            }
            relatedProductDTO.setCategoryId((Long) categoryId);         
        }
        
        if (quantityStr != null) {
            IStandardExpression expression3 = StandardExpressions.getExpressionParser(context.getConfiguration())
                    .parseExpression(context, quantityStr);
            Object quantityExp = expression3.execute(context);
            int quantity = 0;
            if (quantityExp instanceof String) {
                quantity = Integer.parseInt((String)quantityExp);
            } else {
                quantity = ((BigDecimal)expression3.execute(context)).intValue();
            }
            relatedProductDTO.setQuantity(quantity);          
        }       
                
        if (typeStr != null ) {
            IStandardExpression expression4 = StandardExpressions.getExpressionParser(context.getConfiguration())
                    .parseExpression(context, typeStr);
            Object typeExp = expression4.execute(context);
            if (typeExp instanceof String && RelatedProductTypeEnum.getInstance((String)typeExp) != null) {
                relatedProductDTO.setType(RelatedProductTypeEnum.getInstance((String)typeExp));
            }

        }
        
        if ("false".equalsIgnoreCase(tag.getAttributeValue("cumulativeResults"))) {
            relatedProductDTO.setCumulativeResults(false);          
        }
                    
        return relatedProductDTO;
    }
}
