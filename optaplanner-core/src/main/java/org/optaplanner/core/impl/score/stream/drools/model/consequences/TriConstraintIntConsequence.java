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

package org.optaplanner.core.impl.score.stream.drools.model.consequences;

import static java.util.Objects.requireNonNull;

import org.optaplanner.core.api.function.ToIntTriFunction;
import org.optaplanner.core.impl.score.stream.drools.model.TriConstraintModelNode;

final class TriConstraintIntConsequence<A, B, C> implements TriConstraintConsequence<A, B, C>,
        ToIntTriFunction<A, B, C> {

    private final TriConstraintModelNode<A, B, C> terminalNode;
    private final ToIntTriFunction<A, B, C> matchWeighter;

    TriConstraintIntConsequence(TriConstraintModelNode<A, B, C> terminalNode, ToIntTriFunction<A, B, C> matchWeighter) {
        this.terminalNode = requireNonNull(terminalNode);
        this.matchWeighter = requireNonNull(matchWeighter);
    }

    @Override
    public TriConstraintModelNode<A, B, C> getTerminalNode() {
        return terminalNode;
    }

    @Override
    public ConsequenceMatchWeightType getMatchWeightType() {
        return ConsequenceMatchWeightType.INTEGER;
    }

    @Override
    public int applyAsInt(A a, B b, C c) {
        return matchWeighter.applyAsInt(a, b, c);
    }

}
