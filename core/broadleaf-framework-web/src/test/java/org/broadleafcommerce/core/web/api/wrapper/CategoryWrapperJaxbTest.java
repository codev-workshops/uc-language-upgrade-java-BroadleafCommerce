/*
 * #%L
 * BroadleafCommerce Framework Web
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
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
package org.broadleafcommerce.core.web.api.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;

/**
 * Verifies that the REST API wrappers still marshal and unmarshal after the migration of the
 * {@code javax.xml.bind} annotations to {@code jakarta.xml.bind} and the Glassfish JAXB 4 runtime.
 */
public class CategoryWrapperJaxbTest {

  @Test
  public void categoryWrapperRoundTripsThroughJakartaJaxb() throws Exception {
    CategoryWrapper wrapper = new CategoryWrapper();
    setField(wrapper, "id", 42L);
    setField(wrapper, "name", "Hot Sauces");
    setField(wrapper, "active", Boolean.TRUE);

    JAXBContext context = JAXBContext.newInstance(CategoryWrapper.class);
    Marshaller marshaller = context.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(wrapper, writer);
    String xml = writer.toString();

    assertTrue(xml.contains("<category>"));
    assertTrue(xml.contains("<name>Hot Sauces</name>"));

    Unmarshaller unmarshaller = context.createUnmarshaller();
    CategoryWrapper read = (CategoryWrapper) unmarshaller.unmarshal(new StringReader(xml));

    assertEquals(Long.valueOf(42L), getField(read, "id"));
    assertEquals("Hot Sauces", getField(read, "name"));
    assertEquals(Boolean.TRUE, getField(read, "active"));
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = CategoryWrapper.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Object getField(Object target, String name) throws Exception {
    Field field = CategoryWrapper.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }
}
