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
package org.broadleafcommerce.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionHelperTest {

    @Test
    public void aRuntimeExceptionIsReturnedUnchanged() {
        RuntimeException original = new IllegalStateException("boom");
        assertSame(original, ExceptionHelper.refineException(original));
    }

    @Test
    public void aCheckedExceptionIsWrapped() {
        IOException original = new IOException("io");
        RuntimeException refined = ExceptionHelper.refineException(original);
        assertSame(original, refined.getCause());
    }

    @Test
    public void theWrapTypeAndMessageAreHonoured() {
        IOException original = new IOException("io");
        RuntimeException refined =
                ExceptionHelper.refineException(IOException.class, IllegalArgumentException.class, "wrapped", original);

        assertTrue(refined instanceof IllegalArgumentException, refined.getClass().getName());
        assertEquals("wrapped", refined.getMessage());
        assertSame(original, refined.getCause());
    }

    @Test
    public void undeclaredAndInvocationTargetWrappersAreUnwrapped() {
        IOException target = new IOException("io");
        RuntimeException fromUndeclared = ExceptionHelper.refineException(IOException.class,
                IllegalArgumentException.class, new UndeclaredThrowableException(target));
        RuntimeException fromInvocation = ExceptionHelper.refineException(IOException.class,
                IllegalArgumentException.class, new InvocationTargetException(target));

        assertSame(target, fromUndeclared.getCause());
        assertSame(target, fromInvocation.getCause());
    }

    @Test
    public void processExceptionRethrowsTheRefinedType() {
        final IOException original = new IOException("io");
        IOException thrown = assertThrows(IOException.class,
                () -> ExceptionHelper.processException(IOException.class, RuntimeException.class, original));
        assertSame(original, thrown);
    }

    @Test
    public void processExceptionWrapsWhatItCannotRefine() {
        final IllegalStateException original = new IllegalStateException("boom");
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ExceptionHelper.processException(original));
        assertSame(original, thrown);
    }
}
