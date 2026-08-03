/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
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
package org.broadleafcommerce.common.payment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadleafcommerce.common.testsupport.BeanExerciser;
import org.broadleafcommerce.common.testsupport.ClassScanner;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Covers the fluent payment gateway DTOs: each builder method must return the builder it was called on and be
 * readable through its accessor, and the nested builders must be wired back to their parent.
 */
public class PaymentDtoBuilderTest {

    private static List<Class<?>> paymentDtos() {
        List<Class<?>> dtos = new ArrayList<Class<?>>();
        for (Class<?> candidate : ClassScanner.classesUnder("org.broadleafcommerce.common.payment.dto.")) {
            if (!candidate.isInterface() && !candidate.isEnum()
                    && !Modifier.isAbstract(candidate.getModifiers())
                    && Modifier.isPublic(candidate.getModifiers())) {
                dtos.add(candidate);
            }
        }
        return dtos;
    }

    @Test
    public void paymentDtosWereFound() {
        assertFalse(paymentDtos().isEmpty(), "expected the common module to declare payment DTOs");
    }

    @TestFactory
    public List<DynamicTest> builderMethodsRoundTrip() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        for (final Class<?> dto : paymentDtos()) {
            tests.add(DynamicTest.dynamicTest(dto.getName(), () -> BeanExerciser.exerciseFluent(dto)));
        }
        return tests;
    }

    @Test
    public void nestedBuildersReturnToTheirParent() {
        PaymentRequestDTO request = new PaymentRequestDTO();

        assertSame(request, request.billTo().done());
        assertSame(request, request.shipTo().done());
        assertSame(request, request.creditCard().done());
        assertSame(request, request.customer().done());
        assertSame(request, request.subscription().done());
    }

    @Test
    public void anAddressIsOnlyPopulatedOnceAFieldIsSet() {
        AddressDTO<PaymentRequestDTO> address = new AddressDTO<PaymentRequestDTO>();
        assertFalse(address.addressPopulated());

        address.addressLine1("123 Main Street");
        assertTrue(address.addressPopulated());
        assertEquals("123 Main Street", address.getAddressLine1());
    }

    @Test
    public void additionalFieldsAreCollected() {
        AddressDTO<PaymentRequestDTO> address = new AddressDTO<PaymentRequestDTO>();
        address.additionalFields("key", "value");
        assertEquals("value", address.getAdditionalFields().get("key"));
        assertTrue(address.addressPopulated());
    }

    @Test
    public void lineItemsAreAccumulatedOnTheRequest() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        LineItemDTO lineItem = request.lineItem().name("shirt").quantity("2");

        assertSame(request, lineItem.done());
        assertEquals(1, request.getLineItems().size());
        assertEquals("shirt", request.getLineItems().get(0).getName());
    }
}
