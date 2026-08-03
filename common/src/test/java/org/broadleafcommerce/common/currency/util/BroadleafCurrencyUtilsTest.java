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
package org.broadleafcommerce.common.currency.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.broadleafcommerce.common.currency.domain.BroadleafCurrencyImpl;
import org.broadleafcommerce.common.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class BroadleafCurrencyUtilsTest {

    private BroadleafCurrencyImpl currency(String code) {
        BroadleafCurrencyImpl currency = new BroadleafCurrencyImpl();
        currency.setCurrencyCode(code);
        return currency;
    }

    @Test
    public void moneyIsBuiltFromAnAmountAndAnOptionalCurrency() {
        assertNull(BroadleafCurrencyUtils.getMoney(null, currency("EUR")));
        assertEquals(Currency.getInstance("EUR"),
                BroadleafCurrencyUtils.getMoney(BigDecimal.ONE, currency("EUR")).getCurrency());
        assertEquals(Money.defaultCurrency(),
                BroadleafCurrencyUtils.getMoney(BigDecimal.ONE, null).getCurrency());
    }

    @Test
    public void aZeroMoneyIsBuiltForACurrency() {
        assertEquals(new Money(BigDecimal.ZERO, "EUR"), BroadleafCurrencyUtils.getMoney(currency("EUR")));
        assertEquals(new Money(), BroadleafCurrencyUtils.getMoney((BroadleafCurrencyImpl) null));
    }

    @Test
    public void theCurrencyFallsBackToTheDefault() {
        assertEquals(Money.defaultCurrency(), BroadleafCurrencyUtils.getCurrency((Money) null));
        assertEquals(Money.defaultCurrency(), BroadleafCurrencyUtils.getCurrency((BroadleafCurrencyImpl) null));
        assertEquals(Currency.getInstance("EUR"), BroadleafCurrencyUtils.getCurrency(currency("EUR")));
        assertEquals(Currency.getInstance("EUR"),
                BroadleafCurrencyUtils.getCurrency(new Money(BigDecimal.ONE, "EUR")));
    }

    @Test
    public void theUnitAmountFollowsTheFractionDigitsAndSign() {
        assertEquals(new Money(new BigDecimal("0.01"), "USD"),
                BroadleafCurrencyUtils.getUnitAmount(new Money(BigDecimal.ONE, "USD")));
        assertEquals(new Money(new BigDecimal("-0.01"), "USD"),
                BroadleafCurrencyUtils.getUnitAmount(new Money(new BigDecimal("-1"), "USD")));
        assertEquals(new Money(new BigDecimal("0.01"), "EUR"),
                BroadleafCurrencyUtils.getUnitAmount(currency("EUR")));
    }

    @Test
    public void theRemainderIsCalculatedInTheSmallestUnit() {
        assertEquals(0, BroadleafCurrencyUtils.calculateRemainder(null, 3));
        assertEquals(0, BroadleafCurrencyUtils.calculateRemainder(new Money(BigDecimal.ZERO), 3));
        assertEquals(0, BroadleafCurrencyUtils.calculateRemainder(new Money(new BigDecimal("1.00")), 0));
        assertEquals(1, BroadleafCurrencyUtils.calculateRemainder(new Money(new BigDecimal("1.00")), 3));
    }

    @Test
    public void numberFormatsAreCached() {
        Currency usd = Currency.getInstance("USD");
        NumberFormat first = BroadleafCurrencyUtils.getNumberFormatFromCache(Locale.US, usd);
        assertSame(first, BroadleafCurrencyUtils.getNumberFormatFromCache(Locale.US, usd));
        assertEquals(usd, first.getCurrency());
    }
}
