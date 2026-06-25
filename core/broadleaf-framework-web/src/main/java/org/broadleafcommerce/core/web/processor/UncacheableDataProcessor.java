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
import org.apache.commons.lang3.StringUtils;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.security.service.ExploitProtectionService;
import org.broadleafcommerce.common.util.StringUtil;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductOptionXref;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.inventory.service.InventoryService;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.SkuAccessor;
import org.broadleafcommerce.core.web.order.CartState;
import org.broadleafcommerce.core.web.processor.extension.UncacheableDataProcessorExtensionManager;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.web.core.CustomerState;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

/**
 * This processor outputs a SCRIPT tag with JSON data that can be used to update a mostly cached page followed by
 * a call to a javascript function.  The function name is "updateUncacheableData()" by default.
 * 
 * Broadleaf provides this example but most clients with typical customizations will need to create a similar processor
 * to meet their dynamic data caching needs.
 * 
 * The Broadleaf processor works with the sample javacript function in HeatClinic found in heatClinic-UncacheableData.js work 
 * together to update the "In Cart", "Out of Stock", "Welcome {name}", and "Cart Qty" messages.   By doing this, the 
 * category and product pages in HeatClinic can be aggressively cached using the {@link BroadleafCacheProcessor}. 
 * 
 * Example usage on cached pages with dynamic data.   This would generally go after the footer for the page.
 * <pre>
 *  {@code
 *      <blc:uncacheabledata />  
 *  }
 * </pre>
 * @author bpolster
 */
public class UncacheableDataProcessor extends AbstractElementTagProcessor {
    
    @Value("${solr.index.use.sku}")
    protected boolean useSku;

    @Resource(name = "blInventoryService")
    protected InventoryService inventoryService;

    @Resource(name = "blExploitProtectionService")
    protected ExploitProtectionService eps;

    @Resource(name = "blUncacheableDataProcessorExtensionManager")
    protected UncacheableDataProcessorExtensionManager extensionManager;

    private String defaultCallbackFunction = "updateUncacheableData(params)";

    public UncacheableDataProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, "uncacheabledata", true, null, false, 100);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) {
        StringBuffer sb = new StringBuffer();
        sb.append("<SCRIPT>\n");
        sb.append("  var params = \n  ");
        sb.append(buildContentMap(context)).append(";\n  ");
        sb.append(getUncacheableDataFunction(context, tag)).append(";\n");
        sb.append("</SCRIPT>");
                
        structureHandler.replaceWith(sb.toString(), false);
    }

    @SuppressWarnings("unchecked")
    protected String buildContentMap(ITemplateContext context) {
        Map<String, Object> attrMap = new HashMap<String, Object>();
        addCartData(attrMap);
        addCustomerData(attrMap);
        addProductInventoryData(attrMap, context);

        try {
            attrMap.put("csrfToken", eps.getCSRFToken());
            attrMap.put("csrfTokenParameter", eps.getCsrfTokenParameter());
        } catch (ServiceException e) {
            throw new RuntimeException("Could not get a CSRF token for this session", e);
        }
        return StringUtil.getMapAsJson(attrMap);
    }

    @SuppressWarnings("unchecked")
    protected void addProductInventoryData(Map<String, Object> attrMap, ITemplateContext context) {
        List<Long> outOfStockProducts = new ArrayList<Long>();
        List<Long> outOfStockSkus = new ArrayList<Long>();

        Set<Product> allProducts = new HashSet<Product>();
        Set<Sku> allSkus = new HashSet<Sku>();
        Set<Product> products = (Set<Product>) context.getVariable("blcAllDisplayedProducts");
        Set<Sku> skus = (Set<Sku>) context.getVariable("blcAllDisplayedSkus");
        if (!CollectionUtils.isEmpty(products)) {
            allProducts.addAll(products);
        }
        if (!CollectionUtils.isEmpty(skus)) {
            allSkus.addAll(skus);
        }

        extensionManager.getProxy().modifyProductListForInventoryCheck(context, allProducts, allSkus);

        if (!allProducts.isEmpty()) {
            for (Product product : allProducts) {
                if (product.getDefaultSku() != null) {

                    Boolean qtyAvailable = inventoryService.isAvailable(product.getDefaultSku(), 1);
                    if (qtyAvailable != null && !qtyAvailable) {
                        outOfStockProducts.add(product.getId());
                    }
                }
            }
        } else {
            if (!allSkus.isEmpty()) {
                Map<Sku, Integer> inventoryAvailable = inventoryService.retrieveQuantitiesAvailable(allSkus);
                for (Map.Entry<Sku, Integer> entry : inventoryAvailable.entrySet()) {
                    if (entry.getValue() == null || entry.getValue() < 1) {
                        outOfStockSkus.add(entry.getKey().getId());
                    }
                }
            }
        }
        attrMap.put("outOfStockProducts", outOfStockProducts);
        attrMap.put("outOfStockSkus", outOfStockSkus);
    }

    protected void addCartData(Map<String, Object> attrMap) {
        Order cart = CartState.getCart();
        int cartQty = 0;
        List<Long> cartItemIdsWithOptions = new ArrayList<Long>();
        List<Long> cartItemIdsWithoutOptions = new ArrayList<Long>();

        if (cart != null && cart.getOrderItems() != null) {
            cartQty = cart.getItemCount();

            for (OrderItem item : cart.getOrderItems()) {
                if (item instanceof SkuAccessor) {
                    Sku sku = ((SkuAccessor) item).getSku();
                    if (sku != null && sku.getProduct() != null) {
                        if (useSku) {
                            cartItemIdsWithoutOptions.add(sku.getId());
                        } else {
                            Product product = sku.getProduct();
                            List<ProductOptionXref> optionXrefs = product.getProductOptionXrefs();
                            if (optionXrefs == null || optionXrefs.isEmpty()) {
                                cartItemIdsWithoutOptions.add(product.getId());
                            } else {
                                cartItemIdsWithOptions.add(product.getId());
                            } 
                        }
                        
                    }
                }
            }
        }

        attrMap.put("cartItemCount", cartQty);
        attrMap.put("cartItemIdsWithOptions", cartItemIdsWithOptions);
        attrMap.put("cartItemIdsWithoutOptions", cartItemIdsWithoutOptions);
    }
    
    protected void addCustomerData(Map<String, Object> attrMap) {
        Customer customer = CustomerState.getCustomer();
        String firstName = "";
        String lastName = "";
        boolean anonymous = false;

        if (customer != null) {
            if (!StringUtils.isEmpty(customer.getFirstName())) {
                firstName = customer.getFirstName();
            }

            if (!StringUtils.isEmpty(customer.getLastName())) {
                lastName = customer.getLastName();
            }

            if (customer.isAnonymous()) {
                anonymous = true;
            }
        }
        
        attrMap.put("firstName", firstName);
        attrMap.put("lastName", lastName);
        attrMap.put("anonymous", anonymous);
    }
    
    public String getUncacheableDataFunction(ITemplateContext context, IProcessableElementTag tag) {
        if (tag.hasAttribute("callback")) {
            return tag.getAttributeValue("callback");
        } else {
            return getDefaultCallbackFunction();
        }
    }

    public String getDefaultCallbackFunction() {
        return defaultCallbackFunction;
    }

    public void setDefaultCallbackFunction(String defaultCallbackFunction) {
        this.defaultCallbackFunction = defaultCallbackFunction;
    }
}
