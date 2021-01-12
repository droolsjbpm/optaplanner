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

import org.optaplanner.core.api.score.stream.uni.UniConstraintCollector;

import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

final class UniToTriGroupingNode<A, NewA, NewB, NewC>
        extends AbstractConstraintModelGroupingNode<UniLeftHandSide<A>, Function<A, ?>, UniConstraintCollector<A, ?, ?>>
        implements TriConstraintGraphNode {

    UniToTriGroupingNode(UniLeftHandSide<A> leftHandSide, Function<A, NewA> aMapping, Function<A, NewB> bMapping,
            UniConstraintCollector<A, ?, NewC> collector) {
        super(leftHandSide, asList(aMapping, bMapping), singletonList(collector));
    }

}
