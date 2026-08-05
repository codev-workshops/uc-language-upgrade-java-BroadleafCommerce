/*
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
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
package org.broadleafcommerce.common.extensibility.jpa.convert;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

/**
 * Verifies that the weaving pipeline recognizes the {@code jakarta.persistence} type level annotations after the
 * jakarta namespace migration.
 */
public class EntityMarkerClassTransformerTest extends TestCase {

  @Entity
  public static class JakartaEntity {
  }

  @Embeddable
  public static class JakartaEmbeddable {
  }

  @MappedSuperclass
  public static class JakartaMappedSuperclass {
  }

  public static class PlainClass {
  }

  public void testJakartaAnnotatedClassesAreMarkedAsEntities() throws Exception {
    assertTransformed(JakartaEntity.class, true);
    assertTransformed(JakartaEmbeddable.class, true);
    assertTransformed(JakartaMappedSuperclass.class, true);
    assertTransformed(PlainClass.class, false);
  }

  private void assertTransformed(Class<?> clazz, boolean expectedEntity) throws Exception {
    EntityMarkerClassTransformer transformer = new EntityMarkerClassTransformer();
    String internalName = clazz.getName().replace('.', '/');
    transformer.transform(clazz.getClassLoader(), internalName, null, null, readClassBytes(clazz));
    assertEquals(clazz.getName() + " entity detection", expectedEntity,
        transformer.getTransformedEntityClassNames().contains(clazz.getName()));
  }

  private byte[] readClassBytes(Class<?> clazz) throws IOException {
    String resource = clazz.getName().replace('.', '/') + ".class";
    InputStream is = clazz.getClassLoader().getResourceAsStream(resource);
    assertNotNull("class file for " + clazz.getName(), is);
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = is.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    } finally {
      is.close();
    }
  }
}
