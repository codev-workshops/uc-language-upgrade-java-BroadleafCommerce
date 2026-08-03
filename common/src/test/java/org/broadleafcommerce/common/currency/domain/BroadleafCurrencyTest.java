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
package org.broadleafcommerce.common.currency.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.Currency;

public class BroadleafCurrencyTest {

    @Test
    public void javaCurrencyIsResolvedFromTheCurrencyCode() {
        BroadleafCurrencyImpl currency = new BroadleafCurrencyImpl();
        currency.setCurrencyCode("EUR");
        assertEquals(Currency.getInstance("EUR"), currency.getJavaCurrency());
    }

    @Test
    public void javaCurrencyIsNullWithoutACurrencyCode() {
        assertNull(new BroadleafCurrencyImpl().getJavaCurrency());
    }

    @Test
    public void theNullCurrencyIgnoresMutationAndReadsEmpty() {
        NullBroadleafCurrency currency = new NullBroadleafCurrency();
        currency.setCurrencyCode("USD");
        currency.setFriendlyName("US Dollar");
        currency.setDefaultFlag(true);

        assertNull(currency.getCurrencyCode());
        assertNull(currency.getFriendlyName());
        assertNull(currency.getJavaCurrency());
        assertFalse(currency.getDefaultFlag());
    }
}
