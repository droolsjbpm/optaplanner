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

package org.optaplanner.core.impl.score.stream.drools.common.rules;

import org.optaplanner.core.api.function.QuadFunction;
import org.optaplanner.core.api.score.stream.quad.QuadConstraintCollector;

import static java.util.Arrays.asList;

final class QuadToQuadGroupingNode<A, B, C, D, NewA, NewB, NewC, NewD>
        extends
        AbstractConstraintModelGroupingNode<QuadLeftHandSide<A, B, C, D>, QuadFunction<A, B, C, D, ?>, QuadConstraintCollector<A, B, C, D, ?, ?>>
        implements QuadConstraintGraphNode {

    QuadToQuadGroupingNode(QuadLeftHandSide<A, B, C, D> leftHandSide, QuadFunction<A, B, C, D, NewA> aMapping,
            QuadFunction<A, B, C, D, NewB> bMapping, QuadConstraintCollector<A, B, C, D, ?, NewC> cCollector,
            QuadConstraintCollector<A, B, C, D, ?, NewD> dCollector) {
        super(leftHandSide, asList(aMapping, bMapping), asList(cCollector, dCollector));
    }

}
