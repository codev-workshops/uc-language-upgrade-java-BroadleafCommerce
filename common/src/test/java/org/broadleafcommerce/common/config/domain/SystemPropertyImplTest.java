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
package org.broadleafcommerce.common.config.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.broadleafcommerce.common.config.service.type.SystemPropertyFieldType;
import org.junit.jupiter.api.Test;

public class SystemPropertyImplTest {

    @Test
    public void thePropertyTypeIsStoredByItsKey() {
        SystemPropertyImpl property = new SystemPropertyImpl();
        property.setPropertyType(SystemPropertyFieldType.BOOLEAN_TYPE);
        assertSame(SystemPropertyFieldType.BOOLEAN_TYPE, property.getPropertyType());
    }

    @Test
    public void thePropertyTypeDefaultsToString() {
        assertSame(SystemPropertyFieldType.STRING_TYPE, new SystemPropertyImpl().getPropertyType());
    }

    @Test
    public void theMainEntityNameIsTheName() {
        SystemPropertyImpl property = new SystemPropertyImpl();
        property.setName("site.name");
        assertEquals("site.name", property.getMainEntityName());
    }
}
