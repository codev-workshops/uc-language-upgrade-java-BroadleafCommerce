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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StopWatchTest {

    @Test
    public void aNewStopWatchIsRunningAndSplit() {
        StopWatch watch = new StopWatch();

        assertTrue(watch.getTime() >= 0);
        assertTrue(watch.getSplitTime() >= 0);
        assertTrue(watch.getLapTime() >= 0);
        assertTrue(watch.getStartTime() > 0);
        assertNotNull(watch.toString());
        assertNotNull(watch.toSplitString());
        assertNotNull(watch.toLapString());
    }

    @Test
    public void stoppingFreezesTheElapsedTime() {
        StopWatch watch = new StopWatch();
        assertSame(watch, watch.stop());

        long stopped = watch.getTime();
        assertEquals(stopped, watch.getTime());
        assertThrows(IllegalStateException.class, () -> watch.start());
    }

    @Test
    public void aStoppedStopWatchCanBeResetAndRestarted() {
        StopWatch watch = new StopWatch();
        watch.stop();
        assertSame(watch, watch.reset());

        assertEquals(0L, watch.getTime());
        assertThrows(IllegalStateException.class, () -> watch.getStartTime());
        assertSame(watch, watch.start());
        assertTrue(watch.getTime() >= 0);
    }

    @Test
    public void suspendAndResumeKeepTheClockFrozenInBetween() throws Exception {
        StopWatch watch = new StopWatch();
        watch.suspend();
        long suspended = watch.getTime();
        Thread.sleep(5L);

        assertEquals(suspended, watch.getTime());
        assertSame(watch, watch.resume());
        assertThrows(IllegalStateException.class, () -> watch.resume());
    }

    @Test
    public void unsplitInvalidatesTheSplitAccessors() {
        StopWatch watch = new StopWatch();
        assertSame(watch, watch.unsplit());

        assertThrows(IllegalStateException.class, () -> watch.getSplitTime());
        assertThrows(IllegalStateException.class, () -> watch.getLapTime());
        assertThrows(IllegalStateException.class, () -> watch.unsplit());
        assertSame(watch, watch.split());
        assertTrue(watch.getSplitTime() >= 0);
    }

    @Test
    public void theStateGuardsRejectOutOfOrderCalls() {
        StopWatch watch = new StopWatch();
        watch.reset();

        assertThrows(IllegalStateException.class, () -> watch.stop());
        assertThrows(IllegalStateException.class, () -> watch.split());
        assertThrows(IllegalStateException.class, () -> watch.suspend());
    }

    @Test
    public void printHelpersReturnTheStopWatch() {
        StopWatch watch = new StopWatch();

        assertSame(watch, watch.printString("elapsed"));
        assertSame(watch, watch.printSplitString("split"));
        assertSame(watch, watch.printLapString("lap"));
    }
}
