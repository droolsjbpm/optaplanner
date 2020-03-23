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

package org.optaplanner.test.impl.score.stream;

import java.util.function.Function;

import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.score.stream.ConstraintSession;

public final class SingleConstraintVerifier<Solution_>
        extends AbstractConstraintVerifier<SingleConstraintVerifierAssertion<Solution_>,
        SingleConstraintVerifier<Solution_>> {

    private final ConstraintVerifier<Solution_> parent;
    private final ConstraintStreamScoreDirectorFactory<Solution_> constraintStreamScoreDirectorFactory;

    SingleConstraintVerifier(ConstraintVerifier<Solution_> constraintVerifier,
            Function<ConstraintFactory, Constraint> constraintFunction,
            ConstraintStreamImplType constraintStreamImplType) {
        this.parent = constraintVerifier;
        ConstraintProvider constraintProvider = constraintFactory -> new Constraint[] {
                constraintFunction.apply(constraintFactory)
        };
        this.constraintStreamScoreDirectorFactory =
                new ConstraintStreamScoreDirectorFactory<>(parent.getSolutionDescriptor(), constraintProvider,
                        constraintStreamImplType);
    }

    ConstraintVerifier<Solution_> getParent() {
        return parent;
    }

    ConstraintStreamScoreDirectorFactory<Solution_> getConstraintStreamScoreDirectorFactory() {
        return constraintStreamScoreDirectorFactory;
    }

    @Override
    public SingleConstraintVerifierAssertion givenFacts(Object... facts) {
        ConstraintSession<Solution_> constraintSession = constraintStreamScoreDirectorFactory.newConstraintStreamingSession(true, null);
        return null;
    }
}
