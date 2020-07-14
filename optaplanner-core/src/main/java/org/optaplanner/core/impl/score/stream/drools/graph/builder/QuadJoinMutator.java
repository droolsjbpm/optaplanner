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

package org.optaplanner.core.impl.score.stream.drools.graph.builder;

import org.drools.model.PatternDSL;
import org.optaplanner.core.impl.score.stream.drools.graph.nodes.AbstractConstraintModelJoiningNode;
import org.optaplanner.core.impl.score.stream.quad.AbstractQuadJoiner;

public class QuadJoinMutator<A, B, C, D> implements JoinMutator {

    private final AbstractQuadJoiner<A, B, C, D> joiner;

    public QuadJoinMutator(AbstractConstraintModelJoiningNode<D, AbstractQuadJoiner<A, B, C, D>> node) {
        this.joiner = node.get().get(0);
    }

    @Override
    public AbstractRuleBuilder apply(AbstractRuleBuilder leftRuleBuilder, AbstractRuleBuilder rightRuleBuilder) {
        PatternDSL.PatternDef<A> aPattern = leftRuleBuilder.getPrimaryPatterns().get(0);
        PatternDSL.PatternDef<B> bPattern = leftRuleBuilder.getPrimaryPatterns().get(1);
        PatternDSL.PatternDef<C> cPattern = leftRuleBuilder.getPrimaryPatterns().get(2);
        PatternDSL.PatternDef<D> dPattern =
                rightRuleBuilder.getPrimaryPatterns().get(rightRuleBuilder.getPrimaryPatterns().size() - 1);
        dPattern.expr("Filter using " + joiner, aPattern.getFirstVariable(),
                bPattern.getFirstVariable(), cPattern.getFirstVariable(), (d, a, b, c) -> joiner.matches(a, b, c, d));
        return merge(leftRuleBuilder, rightRuleBuilder);
    }

    @Override
    public AbstractRuleBuilder newRuleBuilder(AbstractRuleBuilder leftRuleBuilder, AbstractRuleBuilder rightRuleBuilder) {
        return new QuadRuleBuilder(leftRuleBuilder::generateNextId,
                Math.max(leftRuleBuilder.getExpectedGroupByCount(), rightRuleBuilder.getExpectedGroupByCount()));
    }

}
