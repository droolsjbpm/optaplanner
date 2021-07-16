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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.phase.custom.CustomPhaseConfig;
import org.optaplanner.core.config.phase.loop.LoopPhaseConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import org.optaplanner.core.impl.solver.DefaultSolver;
import org.optaplanner.core.impl.testdata.domain.TestdataEntity;
import org.optaplanner.core.impl.testdata.domain.TestdataSolution;

public class LoopPhaseTest {

    @Test
    public void loop() {
        // Run two custom commands in a loop; each command increments score by 1 above 0.
        CustomPhaseConfig customPhaseConfig = new CustomPhaseConfig()
                .withCustomPhaseCommands(new EntityAddingCustomPhaseCommand());
        LoopPhaseConfig loopPhaseConfig = new LoopPhaseConfig()
                .withPhaseConfigList(Arrays.asList(customPhaseConfig, customPhaseConfig));

        // Terminate after 3 score increments; this ensures that loop termination works inbetween nested phases.
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("4"))
                .withConstraintProviderClass(EntityCountingConstraintProvider.class)
                .withPhases(loopPhaseConfig);

        DefaultSolver<TestdataSolution> solver =
                (DefaultSolver<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig)
                        .buildSolver();
        TestdataSolution solution =
                solver.solve(TestdataSolution.generateSolution(1, 1)); // No entities = score of 1.
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(4)); // We stopped when we were supposed to.

        PhaseCounter<TestdataSolution> phaseCounter = solver.getSolverScope().getPhaseCounter();
        assertThat(phaseCounter.getPhasesEnded()).isEqualTo(4); // 1 loop + 3 iterations.
    }

    private static final class EntityAddingCustomPhaseCommand implements CustomPhaseCommand<TestdataSolution> {

        @Override
        public void changeWorkingSolution(ScoreDirector<TestdataSolution> scoreDirector) {
            TestdataEntity entity = new TestdataEntity(UUID.randomUUID().toString());
            entity.setValue(scoreDirector.getWorkingSolution().getValueList().get(0));
            scoreDirector.beforeEntityAdded(entity);
            scoreDirector.getWorkingSolution().getEntityList().add(entity);
            scoreDirector.afterEntityAdded(entity);
            scoreDirector.triggerVariableListeners();
        }
    }

    public static final class EntityCountingConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] {
                    constraintFactory.from(TestdataEntity.class)
                            .reward("Reward every entity", SimpleScore.ONE)
            };
        }
    }

}
