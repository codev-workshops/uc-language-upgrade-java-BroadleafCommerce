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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadleafcommerce.common.util.TableCreator.Col;
import org.junit.jupiter.api.Test;

public class TableCreatorTest {

    private static Col[] columns() {
        return new Col[] {new Col("Name"), new Col("Value", 10)};
    }

    @Test
    public void headersAndRowsAreRendered() {
        TableCreator table = new TableCreator(columns());
        table.addRow(new Object[] {"currency", "USD"});
        table.addSeparator();

        String rendered = table.toString();
        assertTrue(rendered.contains("Name"), rendered);
        assertTrue(rendered.contains("curr"), rendered);
        assertTrue(rendered.contains("USD"), rendered);
        assertTrue(rendered.contains("----"), rendered);
    }

    @Test
    public void oversizedCellsAreTruncatedToTheColumnWidth() {
        TableCreator table = new TableCreator(new Col[] {new Col("Name", 4)});
        table.addRow(new Object[] {"a-very-long-value"});
        assertTrue(table.toString().contains("| a-ve |"), table.toString());
    }

    @Test
    public void aRowWithTheWrongNumberOfCellsIsRejected() {
        TableCreator table = new TableCreator(columns());
        assertThrows(IllegalArgumentException.class, () -> table.addRow(new Object[] {"only-one"}));
    }

    @Test
    public void headerRowsUseTheGlobalRowHeaderWidth() {
        TableCreator table = new TableCreator(columns()).withGlobalRowHeaderWidth(8);
        table.addRow("header", "data");
        assertTrue(table.toString().contains("| header  data"), table.toString());
    }
}
