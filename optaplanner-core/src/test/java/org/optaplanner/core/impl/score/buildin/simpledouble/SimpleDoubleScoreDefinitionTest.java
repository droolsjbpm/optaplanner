/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.score.buildin.simpledouble;

import org.junit.Test;
import org.optaplanner.core.api.score.buildin.simpledouble.SimpleDoubleScore;
import org.optaplanner.core.config.score.trend.InitializingScoreTrendLevel;
import org.optaplanner.core.impl.score.trend.InitializingScoreTrend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class SimpleDoubleScoreDefinitionTest {

    @Test
    public void getLevelSize() {
        assertThat(new SimpleDoubleScoreDefinition().getLevelsSize()).isEqualTo(1);
    }

    @Test
    public void getLevelLabels() {
        assertArrayEquals(new String[]{"score"}, new SimpleDoubleScoreDefinition().getLevelLabels());
    }

    @Test
    public void buildOptimisticBoundOnlyUp() {
        SimpleDoubleScoreDefinition scoreDefinition = new SimpleDoubleScoreDefinition();
        SimpleDoubleScore optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 1),
                SimpleDoubleScore.valueOfInitialized(-1.7));
        assertThat(optimisticBound.getInitScore()).isEqualTo(0);
        assertThat(optimisticBound.getScore()).isEqualTo(Double.POSITIVE_INFINITY, offset(0.0));
    }

    @Test
    public void buildOptimisticBoundOnlyDown() {
        SimpleDoubleScoreDefinition scoreDefinition = new SimpleDoubleScoreDefinition();
        SimpleDoubleScore optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1),
                SimpleDoubleScore.valueOfInitialized(-1.7));
        assertThat(optimisticBound.getInitScore()).isEqualTo(0);
        assertThat(optimisticBound.getScore()).isEqualTo(-1.7, offset(0.0));
    }

    @Test
    public void buildPessimisticBoundOnlyUp() {
        SimpleDoubleScoreDefinition scoreDefinition = new SimpleDoubleScoreDefinition();
        SimpleDoubleScore pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 1),
                SimpleDoubleScore.valueOfInitialized(-1.7));
        assertThat(pessimisticBound.getInitScore()).isEqualTo(0);
        assertThat(pessimisticBound.getScore()).isEqualTo(-1.7, offset(0.0));
    }

    @Test
    public void buildPessimisticBoundOnlyDown() {
        SimpleDoubleScoreDefinition scoreDefinition = new SimpleDoubleScoreDefinition();
        SimpleDoubleScore pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1),
                SimpleDoubleScore.valueOfInitialized(-1.7));
        assertThat(pessimisticBound.getInitScore()).isEqualTo(0);
        assertThat(pessimisticBound.getScore()).isEqualTo(Double.NEGATIVE_INFINITY, offset(0.0));
    }

}
