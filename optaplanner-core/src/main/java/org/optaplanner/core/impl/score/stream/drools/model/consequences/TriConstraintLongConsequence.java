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

import java.util.function.Supplier;

import org.optaplanner.core.api.function.ToLongTriFunction;
import org.optaplanner.core.impl.score.stream.drools.model.nodes.TriConstraintModelNode;

final class TriConstraintLongConsequence<A, B, C> implements TriConstraintConsequence<A, B, C>,
        Supplier<ToLongTriFunction<A, B, C>> {

    private final TriConstraintModelNode<A, B, C> terminalNode;
    private final ToLongTriFunction<A, B, C> matchWeighter;

    TriConstraintLongConsequence(TriConstraintModelNode<A, B, C> terminalNode,
            ToLongTriFunction<A, B, C> matchWeighter) {
        this.terminalNode = requireNonNull(terminalNode);
        this.matchWeighter = requireNonNull(matchWeighter);
    }

    @Override
    public TriConstraintModelNode<A, B, C> getTerminalNode() {
        return terminalNode;
    }

    @Override
    public ConsequenceMatchWeightType getMatchWeightType() {
        return ConsequenceMatchWeightType.LONG;
    }

    @Override
    public ToLongTriFunction<A, B, C> get() {
        return matchWeighter;
    }
}
