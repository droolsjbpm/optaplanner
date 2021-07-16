/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.phase;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

public class PhaseCounterTest {

    @Test
    <Solution_> void phaseTransitions() {
        PhaseCounter<Solution_> phaseCounter = new PhaseCounter<>();
        assertSoftly(softly -> {
            softly.assertThat(phaseCounter.getPhasesStarted()).isEqualTo(0);
            softly.assertThat(phaseCounter.getPhasesEnded()).isEqualTo(0);
        });

        phaseCounter.solvingStarted(null);
        assertSoftly(softly -> {
            softly.assertThat(phaseCounter.getPhasesStarted()).isEqualTo(0);
            softly.assertThat(phaseCounter.getPhasesEnded()).isEqualTo(0);
        });

        phaseCounter.phaseStarted(null);
        assertSoftly(softly -> {
            softly.assertThat(phaseCounter.getPhasesStarted()).isEqualTo(1);
            softly.assertThat(phaseCounter.getPhasesEnded()).isEqualTo(0);
        });

        phaseCounter.phaseEnded(null);
        assertSoftly(softly -> {
            softly.assertThat(phaseCounter.getPhasesStarted()).isEqualTo(1);
            softly.assertThat(phaseCounter.getPhasesEnded()).isEqualTo(1);
        });

        phaseCounter.solvingEnded(null);
        assertSoftly(softly -> {
            softly.assertThat(phaseCounter.getPhasesStarted()).isEqualTo(1);
            softly.assertThat(phaseCounter.getPhasesEnded()).isEqualTo(1);
        });

        phaseCounter.solvingStarted(null);
        assertSoftly(softly -> {
            softly.assertThat(phaseCounter.getPhasesStarted()).isEqualTo(0);
            softly.assertThat(phaseCounter.getPhasesEnded()).isEqualTo(0);
        });

    }

}
