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
package org.broadleafcommerce.common.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.Currency;

public class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    public void constructorsProduceExpectedAmounts() {
        assertEquals(new BigDecimal("0.00"), new Money().getAmount());
        assertEquals(new BigDecimal("5.00"), new Money(5).getAmount());
        assertEquals(new BigDecimal("5.00"), new Money(5L).getAmount());
        assertEquals(new BigDecimal("5.25"), new Money(5.25d).getAmount());
        assertEquals(new BigDecimal("5.25"), new Money("5.25").getAmount());
        assertEquals(new BigDecimal("5.25"), new Money(new BigDecimal("5.25")).getAmount());
        assertEquals(USD, new Money(BigDecimal.ONE, "USD").getCurrency());
        assertEquals(EUR, new Money(1d, EUR).getCurrency());
        assertEquals(EUR, new Money(1L, EUR).getCurrency());
        assertEquals(EUR, new Money("1", EUR).getCurrency());
    }

    @Test
    public void nullAmountIsRejected() {
        assertThrows(NullPointerException.class, () -> new Money((BigDecimal) null, USD));
    }

    @Test
    public void arithmeticOperations() {
        Money five = new Money(5, USD);
        Money two = new Money(2, USD);

        assertEquals(new Money(7, USD), five.add(two));
        assertEquals(new Money(3, USD), five.subtract(two));
        assertEquals(new Money(10, USD), five.multiply(2));
        assertEquals(new Money(10, USD), five.multiply(2L));
        assertEquals(new Money(new BigDecimal("2.50"), USD), five.divide(2));
        assertEquals(new Money(new BigDecimal("2.50"), USD), five.divide(2d));
        assertEquals(new Money(-5, USD), five.negate());
        assertEquals(five, five.negate().abs());
        assertEquals(new Money(new BigDecimal("2.50"), USD), five.multiply(0.5d));
    }

    @Test
    public void comparisonsAndPredicates() {
        Money five = new Money(5, USD);
        Money two = new Money(2, USD);

        assertTrue(five.greaterThan(two));
        assertTrue(five.greaterThan(new BigDecimal("2")));
        assertTrue(five.greaterThanOrEqual(two));
        assertTrue(five.greaterThanOrEqual(new BigDecimal("2")));
        assertTrue(two.lessThan(five));
        assertTrue(two.lessThan(new BigDecimal("5")));
        assertTrue(two.lessThanOrEqual(five));
        assertTrue(two.lessThanOrEqual(new BigDecimal("5")));
        assertEquals(five, five.max(two));
        assertEquals(two, five.min(two));
        assertEquals(five, five.min(null));
        assertEquals(five, five.max(null));
        assertTrue(new Money(0, USD).isZero());
        assertTrue(five.zero().isZero());
        assertFalse(five.isZero());
        assertEquals(1, five.compareTo(two));
        assertEquals(1, five.compareTo(BigDecimal.ONE));
    }

    @Test
    public void staticHelpers() {
        Money five = new Money(5, USD);
        Money two = new Money(2, USD);

        assertEquals(five, Money.abs(new Money(-5, USD)));
        assertEquals(two, Money.min(five, two));
        assertEquals(five, Money.max(five, two));
        assertEquals(new BigDecimal("5.00"), Money.toAmount(five));
        assertEquals(USD, Money.toCurrency(five));
        assertNull(Money.toAmount(null));
        assertNull(Money.toCurrency(null));
        assertNotNull(Money.defaultCurrency());
    }

    @Test
    public void cloneCopiesAmountAndCurrency() {
        Money five = new Money(5, EUR);
        assertEquals(five, five.clone());
    }

    /**
     * Documents current behaviour: {@link Money#writeExternal} serialises the amount as a float and
     * does not serialise the currency at all, so an externalised Money does not round trip. This is a
     * pre-existing defect, asserted here rather than fixed.
     */
    @Test
    public void externalizationLosesPrecisionAndCurrency() throws Exception {
        Money original = new Money(new BigDecimal("12.34"), EUR);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Money read = (Money) in.readObject();
            assertNotEquals(original.getAmount(), read.getAmount());
            assertNotEquals(EUR, read.getCurrency());
        }
    }

    @Test
    public void addingToTheZeroConstantAdoptsTheOtherCurrency() {
        assertEquals(EUR, Money.ZERO.add(new Money(1, EUR)).getCurrency());
        assertEquals(EUR, Money.ZERO.subtract(new Money(1, EUR)).getCurrency());
        Money eur = new Money(1, EUR);
        assertEquals(eur, eur.add(Money.ZERO));
        assertEquals(eur, eur.subtract(Money.ZERO));
    }

    @Test
    public void zeroFactories() {
        assertTrue(Money.zero(USD).isZero());
        assertTrue(Money.zero("USD").isZero());
        assertEquals(USD, Money.zero(USD).getCurrency());
    }

    @Test
    public void abs() {
        assertEquals(new Money(5, USD), new Money(-5, USD).abs());
    }

    @Test
    public void equalsHashCodeAndToString() {
        Money five = new Money(5, USD);
        assertEquals(five, five);
        assertEquals(five, new Money(5, USD));
        assertNotEquals(five, new Money(6, USD));
        assertNotEquals(five, null);
        assertNotEquals(five, "5");
        assertEquals(new Money(5, USD).hashCode(), five.hashCode());
        assertNotNull(five.toString());
        assertNotNull(five.stringValue());
    }

    @Test
    public void numberConversions() {
        Money money = new Money(new BigDecimal("5.75"), USD);
        assertEquals(5.75d, money.doubleValue(), 0.0001d);
    }

    @Test
    public void defaultCurrencyIsUsedWhenNoneSupplied() {
        assertNotNull(new Money(1).getCurrency());
    }

    @Test
    public void mismatchedCurrenciesAreRejected() {
        Money usd = new Money(1, USD);
        Money eur = new Money(1, EUR);
        assertThrows(UnsupportedOperationException.class, () -> usd.add(eur));
        assertThrows(UnsupportedOperationException.class, () -> usd.subtract(eur));
    }
}
