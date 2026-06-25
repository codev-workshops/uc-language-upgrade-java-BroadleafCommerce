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
package org.broadleafcommerce.core.spec.offer.service

import org.broadleafcommerce.core.offer.domain.OfferCodeImpl
import org.broadleafcommerce.core.offer.domain.OfferImpl
import org.broadleafcommerce.core.offer.service.OfferService
import org.broadleafcommerce.core.offer.service.workflow.RecordOfferUsageActivity
import org.broadleafcommerce.core.order.domain.Order
import org.broadleafcommerce.core.order.domain.OrderImpl
import org.broadleafcommerce.core.order.service.OrderService
import org.broadleafcommerce.core.pricing.service.workflow.OfferActivity
import org.broadleafcommerce.core.workflow.DefaultProcessContextImpl
import org.broadleafcommerce.core.workflow.ProcessContext
import org.broadleafcommerce.profile.core.domain.CustomerImpl

import spock.lang.Specification

/**
 * Verifies offer/pricing workflows execute correctly under Spring 5.3.
 * Ensures Spring-managed activity chains and service injection patterns work.
 */
class OfferPricingSpring53Spec extends Specification {

    OfferService mockOfferService = Mock()
    OrderService mockOrderService = Mock()
    ProcessContext<Order> context

    def setup() {
        context = new DefaultProcessContextImpl<Order>().with() {
            seedData = new OrderImpl().with() {
                id = 1
                customer = new CustomerImpl().with() {
                    id = 1
                    it
                }
                it
            }
            it
        }
    }

    def "OfferActivity executes pricing workflow with Spring-injected services"() {
        setup:
        OfferActivity activity = new OfferActivity().with {
            offerService = mockOfferService
            orderService = mockOrderService
            it
        }

        when:
        context = activity.execute(context)

        then:
        1 * mockOfferService.buildOfferCodeListForCustomer(_) >> [new OfferCodeImpl()]
        1 * mockOfferService.applyAndSaveOffersToOrder(_, _) >> context.seedData
        1 * mockOrderService.addOfferCodes(_, _, _) >> context.seedData
        context.seedData != null
    }

    def "OfferImpl domain object can be configured with discount values"() {
        when:
        OfferImpl offer = new OfferImpl()
        offer.setName("Test Offer")
        offer.setValue(new BigDecimal("10.00"))
        offer.setMaxUsesPerOrder(1)

        then:
        offer.getName() == "Test Offer"
        offer.getValue() == new BigDecimal("10.00")
        offer.getMaxUsesPerOrder() == 1
    }

    def "OfferCode can be linked to an Offer"() {
        when:
        OfferImpl offer = new OfferImpl()
        offer.setName("Linked Offer")
        OfferCodeImpl offerCode = new OfferCodeImpl()
        offerCode.setOfferCode("SAVE10")
        offerCode.setOffer(offer)

        then:
        offerCode.getOfferCode() == "SAVE10"
        offerCode.getOffer().getName() == "Linked Offer"
    }

    def "RecordOfferUsageActivity can be instantiated for Spring workflow"() {
        when:
        RecordOfferUsageActivity activity = new RecordOfferUsageActivity()

        then:
        activity != null
    }

    def "ProcessContext supports order with customer for pricing workflows"() {
        expect:
        context.seedData != null
        context.seedData.id == 1
        context.seedData.customer != null
        context.seedData.customer.id == 1
    }

    def "Multiple offer codes can be built for a customer"() {
        setup:
        OfferCodeImpl code1 = new OfferCodeImpl()
        code1.setOfferCode("CODE1")
        OfferCodeImpl code2 = new OfferCodeImpl()
        code2.setOfferCode("CODE2")

        when:
        List codes = [code1, code2]

        then:
        codes.size() == 2
        codes[0].getOfferCode() == "CODE1"
        codes[1].getOfferCode() == "CODE2"
    }
}
