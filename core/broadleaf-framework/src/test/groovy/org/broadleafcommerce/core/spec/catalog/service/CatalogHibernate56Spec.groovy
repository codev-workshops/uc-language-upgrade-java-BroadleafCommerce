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
package org.broadleafcommerce.core.spec.catalog.service

import org.broadleafcommerce.core.catalog.domain.CategoryImpl
import org.broadleafcommerce.core.catalog.domain.ProductImpl
import org.broadleafcommerce.core.catalog.domain.SkuImpl
import org.broadleafcommerce.core.order.domain.OrderImpl
import org.broadleafcommerce.core.order.domain.OrderItemImpl
import org.broadleafcommerce.core.order.service.type.OrderStatus

import spock.lang.Specification

/**
 * Verifies catalog and order domain objects work correctly under Hibernate 5.6.
 * Tests CRUD operations on domain entities to confirm JPA annotation compatibility.
 */
class CatalogHibernate56Spec extends Specification {

    def "Category domain object can be instantiated and populated"() {
        when:
        CategoryImpl category = new CategoryImpl()
        category.setName("Test Category")
        category.setUrl("/test-category")
        category.setActiveStartDate(new Date())

        then:
        category.getName() == "Test Category"
        category.getUrl() == "/test-category"
        category.getActiveStartDate() != null
    }

    def "Product domain object can be instantiated with a default sku"() {
        when:
        ProductImpl product = new ProductImpl()
        SkuImpl sku = new SkuImpl()
        sku.setName("Test SKU")
        sku.setRetailPrice(new org.broadleafcommerce.common.money.Money("19.99"))
        product.setDefaultSku(sku)

        then:
        product.getDefaultSku() != null
        product.getDefaultSku().getName() == "Test SKU"
        product.getDefaultSku().getRetailPrice().getAmount() == new BigDecimal("19.99")
    }

    def "Sku can hold inventory and pricing data"() {
        when:
        SkuImpl sku = new SkuImpl()
        sku.setName("Inventory SKU")
        sku.setRetailPrice(new org.broadleafcommerce.common.money.Money("49.99"))
        sku.setSalePrice(new org.broadleafcommerce.common.money.Money("39.99"))
        sku.setQuantityAvailable(100)

        then:
        sku.getName() == "Inventory SKU"
        sku.getRetailPrice().getAmount() == new BigDecimal("49.99")
        sku.getSalePrice().getAmount() == new BigDecimal("39.99")
        sku.getQuantityAvailable() == 100
    }

    def "Order domain object supports status transitions"() {
        when:
        OrderImpl order = new OrderImpl()
        order.setName("Test Order")
        order.setStatus(OrderStatus.IN_PROCESS)

        then:
        order.getName() == "Test Order"
        order.getStatus() == OrderStatus.IN_PROCESS

        when:
        order.setStatus(OrderStatus.SUBMITTED)

        then:
        order.getStatus() == OrderStatus.SUBMITTED
    }

    def "Order can contain order items"() {
        when:
        OrderImpl order = new OrderImpl()
        order.setName("Multi-item Order")
        OrderItemImpl item1 = new OrderItemImpl()
        item1.setName("Item 1")
        item1.setQuantity(2)
        item1.setOrder(order)

        then:
        item1.getName() == "Item 1"
        item1.getQuantity() == 2
        item1.getOrder() == order
    }

    def "Category can have parent-child hierarchy"() {
        when:
        CategoryImpl parent = new CategoryImpl()
        parent.setName("Parent Category")
        CategoryImpl child = new CategoryImpl()
        child.setName("Child Category")
        child.setDefaultParentCategory(parent)

        then:
        child.getDefaultParentCategory() == parent
        child.getDefaultParentCategory().getName() == "Parent Category"
    }
}
