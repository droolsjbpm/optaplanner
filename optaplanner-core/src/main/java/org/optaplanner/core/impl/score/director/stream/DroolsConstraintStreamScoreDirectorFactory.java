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

package org.optaplanner.core.impl.score.director.stream;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.stream.drools.DroolsConstraintFactory;
import org.optaplanner.core.impl.score.stream.drools.DroolsConstraintSessionFactory;

public final class DroolsConstraintStreamScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> {

    public DroolsConstraintStreamScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintProvider constraintProvider, boolean droolsAlphaNetworkCompilationEnabled) {
        super(solutionDescriptor, constraintProvider,
                () -> new DroolsConstraintFactory<>(solutionDescriptor, droolsAlphaNetworkCompilationEnabled));
    }

    @Override
    public DroolsConstraintStreamScoreDirector<Solution_, Score_> buildScoreDirector(boolean lookUpEnabled,
            boolean constraintMatchEnabledPreference) {
        return new DroolsConstraintStreamScoreDirector<>(this, lookUpEnabled, constraintMatchEnabledPreference);
    }

    public DroolsConstraintSessionFactory.SessionDescriptor<Score_> newConstraintStreamingSession(boolean constraintMatchEnabled, Solution_ workingSolution) {
        return (DroolsConstraintSessionFactory.SessionDescriptor<Score_>) getConstraintSessionFactory()
                .buildSession(constraintMatchEnabled, workingSolution);
    }

}
