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

package org.optaplanner.core.impl.phase.loop;

import java.util.List;
import java.util.stream.Collectors;

import org.optaplanner.core.config.phase.loop.LoopPhaseConfig;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.heuristic.HeuristicConfigPolicy;
import org.optaplanner.core.impl.phase.AbstractPhase;
import org.optaplanner.core.impl.phase.AbstractPhaseFactory;
import org.optaplanner.core.impl.phase.PhaseCounter;
import org.optaplanner.core.impl.phase.PhaseFactory;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.termination.Termination;

public class LoopPhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, LoopPhaseConfig> {

    public LoopPhaseFactory(LoopPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public LoopPhase<Solution_> buildPhase(PhaseCounter<Solution_> phaseCounter,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> solverTermination) {
        if (ConfigUtils.isEmptyCollection(phaseConfig.getPhaseConfigList())) {
            throw new IllegalArgumentException(
                    "Configure at least 1 phase in the <" + LoopPhaseConfig.XML_ELEMENT_NAME + "> configuration.");
        }

        HeuristicConfigPolicy<Solution_> phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        Termination<Solution_> loopTermination = buildPhaseTermination(phaseConfigPolicy, solverTermination);
        List<AbstractPhase<Solution_>> phaseList = phaseConfig.getPhaseConfigList()
                .stream()
                .map(PhaseFactory::<Solution_> create)
                .map(phaseFactory -> (AbstractPhase<Solution_>) phaseFactory.buildPhase(phaseCounter, solverConfigPolicy,
                        bestSolutionRecaller, loopTermination))
                .collect(Collectors.toList());
        return new DefaultLoopPhase<>(phaseCounter, solverConfigPolicy.getLogIndentation(), bestSolutionRecaller,
                loopTermination, phaseList);
    }

}
