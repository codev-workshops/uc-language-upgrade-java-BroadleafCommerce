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
package org.broadleafcommerce.common.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtensionManagerTest {

    /** Handler contract used by the manager under test. */
    public interface SampleHandler extends ExtensionHandler {

        ExtensionResultStatusType doWork(List<String> visited);
    }

    public static class SampleManager extends ExtensionManager<SampleHandler> {

        private final boolean continueOnHandled;

        public SampleManager() {
            this(false);
        }

        public SampleManager(boolean continueOnHandled) {
            super(SampleHandler.class);
            this.continueOnHandled = continueOnHandled;
        }

        @Override
        public boolean continueOnHandled() {
            return continueOnHandled;
        }
    }

    private static class RecordingHandler implements SampleHandler {

        private final String name;
        private final int priority;
        private final boolean enabled;
        private final ExtensionResultStatusType result;

        RecordingHandler(String name, int priority, boolean enabled, ExtensionResultStatusType result) {
            this.name = name;
            this.priority = priority;
            this.enabled = enabled;
            this.result = result;
        }

        @Override
        public ExtensionResultStatusType doWork(List<String> visited) {
            visited.add(name);
            return result;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    private static class SecondHandler extends RecordingHandler {

        SecondHandler(String name, int priority, boolean enabled, ExtensionResultStatusType result) {
            super(name, priority, enabled, result);
        }
    }

    @Test
    public void handlersAreRegisteredOncePerTypeAndSortedByPriority() {
        SampleManager manager = new SampleManager();
        RecordingHandler low = new RecordingHandler("low", 10, true, ExtensionResultStatusType.NOT_HANDLED);
        SecondHandler high = new SecondHandler("high", 1, true, ExtensionResultStatusType.NOT_HANDLED);

        assertTrue(manager.registerHandler(low));
        assertTrue(manager.registerHandler(high));
        assertFalse(manager.registerHandler(new RecordingHandler("duplicate", 5, true, null)));

        assertEquals(Arrays.asList(high, low), manager.getHandlers());
    }

    @Test
    public void theProxyVisitsEveryEnabledHandler() {
        SampleManager manager = new SampleManager();
        manager.registerHandler(new RecordingHandler("first", 1, true, ExtensionResultStatusType.NOT_HANDLED));
        manager.registerHandler(new SecondHandler("second", 2, true, ExtensionResultStatusType.NOT_HANDLED));

        List<String> visited = new ArrayList<String>();
        assertEquals(ExtensionResultStatusType.NOT_HANDLED, manager.getProxy().doWork(visited));
        assertEquals(Arrays.asList("first", "second"), visited);
    }

    @Test
    public void disabledHandlersAreSkipped() {
        SampleManager manager = new SampleManager();
        manager.registerHandler(new RecordingHandler("disabled", 1, false, ExtensionResultStatusType.HANDLED));

        List<String> visited = new ArrayList<String>();
        assertEquals(ExtensionResultStatusType.NOT_HANDLED, manager.getProxy().doWork(visited));
        assertTrue(visited.isEmpty());
    }

    @Test
    public void handlingStopsAfterAHandledResultByDefault() {
        SampleManager manager = new SampleManager();
        manager.registerHandler(new RecordingHandler("first", 1, true, ExtensionResultStatusType.HANDLED));
        manager.registerHandler(new SecondHandler("second", 2, true, ExtensionResultStatusType.NOT_HANDLED));

        List<String> visited = new ArrayList<String>();
        assertEquals(ExtensionResultStatusType.HANDLED, manager.getProxy().doWork(visited));
        assertEquals(Arrays.asList("first"), visited);
    }

    @Test
    public void aManagerCanBeConfiguredToContinueOnHandled() {
        SampleManager manager = new SampleManager(true);
        manager.registerHandler(new RecordingHandler("first", 1, true, ExtensionResultStatusType.HANDLED));
        manager.registerHandler(new SecondHandler("second", 2, true, ExtensionResultStatusType.HANDLED));

        List<String> visited = new ArrayList<String>();
        assertEquals(ExtensionResultStatusType.HANDLED, manager.getProxy().doWork(visited));
        assertEquals(Arrays.asList("first", "second"), visited);
    }

    @Test
    public void handledStopAlwaysStops() {
        SampleManager manager = new SampleManager(true);
        manager.registerHandler(new RecordingHandler("first", 1, true, ExtensionResultStatusType.HANDLED_STOP));
        manager.registerHandler(new SecondHandler("second", 2, true, ExtensionResultStatusType.HANDLED));

        List<String> visited = new ArrayList<String>();
        assertEquals(ExtensionResultStatusType.HANDLED, manager.getProxy().doWork(visited));
        assertEquals(Arrays.asList("first"), visited);
    }

    @Test
    public void aHandlerFailureIsUnwrappedFromTheProxy() {
        SampleManager manager = new SampleManager();
        manager.registerHandler(new RecordingHandler("boom", 1, true, ExtensionResultStatusType.HANDLED) {
            @Override
            public ExtensionResultStatusType doWork(List<String> visited) {
                throw new IllegalStateException("boom");
            }
        });

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> manager.getProxy().doWork(new ArrayList<String>()));
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    public void handlersCanBeReplacedWholesale() {
        SampleManager manager = new SampleManager();
        List<SampleHandler> handlers =
                Arrays.<SampleHandler>asList(new RecordingHandler("only", 1, true, ExtensionResultStatusType.HANDLED));
        manager.setHandlers(handlers);
        assertSame(handlers.get(0), manager.getHandlers().get(0));
    }

    @Test
    public void aManagerHasNoPriorityOfItsOwn() {
        assertThrows(UnsupportedOperationException.class, () -> new SampleManager().getPriority());
    }

    @Test
    public void shouldContinueReflectsTheResultStatus() {
        SampleManager manager = new SampleManager();
        assertTrue(manager.shouldContinue(null, null, null, null));
        assertTrue(manager.shouldContinue(ExtensionResultStatusType.NOT_HANDLED, null, null, null));
        assertFalse(manager.shouldContinue(ExtensionResultStatusType.HANDLED, null, null, null));
        assertFalse(manager.shouldContinue(ExtensionResultStatusType.HANDLED_STOP, null, null, null));
        assertTrue(new SampleManager(true).shouldContinue(ExtensionResultStatusType.HANDLED, null, null, null));
    }
}
