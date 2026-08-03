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
package org.broadleafcommerce.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class UnitOfMeasureUtilTest {

    private static final double TOLERANCE = 0.0001d;

    @Test
    public void weightConversions() {
        assertEquals(0.45359237d, UnitOfMeasureUtil.convertKilogramsToPounds(BigDecimal.ONE).doubleValue(), TOLERANCE);
        assertEquals(2.20462262185d, UnitOfMeasureUtil.convertPoundsToKilograms(BigDecimal.ONE).doubleValue(), TOLERANCE);
        assertEquals(16d, UnitOfMeasureUtil.convertPoundsToOunces(BigDecimal.ONE).doubleValue(), TOLERANCE);
        assertEquals(0.0625d, UnitOfMeasureUtil.convertOuncesToPounds(BigDecimal.ONE).doubleValue(), TOLERANCE);
    }

    @Test
    public void lengthConversions() {
        assertEquals(0.3048d, UnitOfMeasureUtil.convertFeetToMeters(BigDecimal.ONE).doubleValue(), TOLERANCE);
        assertEquals(3.28084d, UnitOfMeasureUtil.convertMetersToFeet(BigDecimal.ONE).doubleValue(), TOLERANCE);
        assertEquals(0.083333d, UnitOfMeasureUtil.convertInchesToFeet(BigDecimal.ONE).doubleValue(), TOLERANCE);
        assertEquals(12d, UnitOfMeasureUtil.convertFeetToInches(BigDecimal.ONE).doubleValue(), TOLERANCE);
    }

    @Test
    public void poundsArePassedThroughAndKilogramsConverted() {
        BigDecimal weight = new BigDecimal("2.5");
        assertEquals(2.5d, UnitOfMeasureUtil.findPounds(weight, WeightUnitOfMeasureType.POUNDS).doubleValue(), TOLERANCE);
        assertEquals(1.1339809d,
                UnitOfMeasureUtil.findPounds(weight, WeightUnitOfMeasureType.KILOGRAMS).doubleValue(), TOLERANCE);
        assertEquals(2, UnitOfMeasureUtil.findWholePounds(weight, WeightUnitOfMeasureType.POUNDS));
        assertEquals(1, UnitOfMeasureUtil.findWholePounds(weight, WeightUnitOfMeasureType.KILOGRAMS));
    }

    @Test
    public void ounceHelpers() {
        BigDecimal weight = new BigDecimal("2.5");
        assertEquals(40d, UnitOfMeasureUtil.findOunces(weight, WeightUnitOfMeasureType.POUNDS).doubleValue(), TOLERANCE);
        assertEquals(8d, UnitOfMeasureUtil.findRemainingOunces(weight, WeightUnitOfMeasureType.POUNDS).doubleValue(),
                TOLERANCE);
        assertEquals(18.1436948d,
                UnitOfMeasureUtil.findOunces(weight, WeightUnitOfMeasureType.KILOGRAMS).doubleValue(), TOLERANCE);
        assertEquals(2.1436948d,
                UnitOfMeasureUtil.findRemainingOunces(weight, WeightUnitOfMeasureType.KILOGRAMS).doubleValue(),
                TOLERANCE);
    }

    @Test
    public void inchesAreDerivedFromEveryDimensionUnit() {
        BigDecimal one = BigDecimal.ONE;
        assertEquals(1d, UnitOfMeasureUtil.findInches(one, DimensionUnitOfMeasureType.INCHES).doubleValue(), TOLERANCE);
        assertEquals(12d, UnitOfMeasureUtil.findInches(one, DimensionUnitOfMeasureType.FEET).doubleValue(), TOLERANCE);
        assertEquals(39.37008d, UnitOfMeasureUtil.findInches(one, DimensionUnitOfMeasureType.METERS).doubleValue(),
                TOLERANCE);
        assertEquals(0.3937008d,
                UnitOfMeasureUtil.findInches(one, DimensionUnitOfMeasureType.CENTIMETERS).doubleValue(), TOLERANCE);
    }
}
