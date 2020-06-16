/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.config.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.optaplanner.core.impl.util.Util.assertEquals;
import static org.optaplanner.core.impl.util.Util.assertNotNull;
import static org.optaplanner.core.impl.util.Util.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.config.heuristic.policy.HeuristicConfigPolicy;
import org.optaplanner.core.impl.solver.termination.Termination;
import org.optaplanner.core.impl.solver.termination.TimeMillisSpentTermination;
import org.optaplanner.core.impl.solver.termination.UnimprovedTimeMillisSpentTermination;

public class TerminationConfigTest {

    @Test
    public void spentLimit() {
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setSpentLimit(Duration.ofMillis(5L)
                .plusSeconds(4L)
                .plusMinutes(3L)
                .plusHours(2L)
                .plusDays(1L));
        Termination termination = terminationConfig.buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat((Object) termination)
                .isInstanceOf(TimeMillisSpentTermination.class);
        assertEquals(93784005L, ((TimeMillisSpentTermination) termination).getTimeMillisSpentLimit());
    }

    @Test
    public void spentLimitWithoutJavaTime() {
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setMillisecondsSpentLimit(5L);
        terminationConfig.setSecondsSpentLimit(4L);
        terminationConfig.setMinutesSpentLimit(3L);
        terminationConfig.setHoursSpentLimit(2L);
        terminationConfig.setDaysSpentLimit(1L);
        Termination termination = terminationConfig.buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat((Object) termination)
                .isInstanceOf(TimeMillisSpentTermination.class);
        assertEquals(93784005L, ((TimeMillisSpentTermination) termination).getTimeMillisSpentLimit());
    }

    @Test
    public void overwriteSpentLimit() {
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setMinutesSpentLimit(1L);
        assertNotNull(terminationConfig.getMinutesSpentLimit());
        terminationConfig.overwriteSpentLimit(Duration.ofHours(2L));
        assertNull(terminationConfig.getMinutesSpentLimit());
    }

    @Test
    public void unimprovedSpentLimit() {
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setUnimprovedSpentLimit(Duration.ofMillis(5L)
                .plusSeconds(4L)
                .plusMinutes(3L)
                .plusHours(2L)
                .plusDays(1L));
        Termination termination = terminationConfig.buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat((Object) termination)
                .isInstanceOf(UnimprovedTimeMillisSpentTermination.class);
        assertEquals(93784005L, ((UnimprovedTimeMillisSpentTermination) termination).getUnimprovedTimeMillisSpentLimit());
    }

    @Test
    public void unimprovedSpentLimitWithoutJavaTime() {
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setUnimprovedMillisecondsSpentLimit(5L);
        terminationConfig.setUnimprovedSecondsSpentLimit(4L);
        terminationConfig.setUnimprovedMinutesSpentLimit(3L);
        terminationConfig.setUnimprovedHoursSpentLimit(2L);
        terminationConfig.setUnimprovedDaysSpentLimit(1L);
        Termination termination = terminationConfig.buildTermination(mock(HeuristicConfigPolicy.class));
        assertThat((Object) termination)
                .isInstanceOf(UnimprovedTimeMillisSpentTermination.class);
        assertEquals(93784005L, ((UnimprovedTimeMillisSpentTermination) termination).getUnimprovedTimeMillisSpentLimit());
    }

    @Test
    public void overwriteUnimprovedSpentLimit() {
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setUnimprovedMinutesSpentLimit(1L);
        assertNotNull(terminationConfig.getUnimprovedMinutesSpentLimit());
        terminationConfig.overwriteUnimprovedSpentLimit(Duration.ofHours(2L));
        assertNull(terminationConfig.getUnimprovedMinutesSpentLimit());
    }

}
