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

package org.optaplanner.core.impl.score.stream.drools.common.rules;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.drools.model.PatternDSL.from;
import static org.drools.model.PatternDSL.groupBy;
import static org.drools.model.PatternDSL.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.drools.model.PatternDSL;
import org.drools.model.Variable;
import org.drools.model.view.ViewItem;
import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.core.impl.score.stream.drools.common.BiTuple;

final class TriGroupBy2Map0CollectFastMutator<A, B, C, NewA, NewB> extends AbstractTriGroupByMutator {

    private final TriFunction<A, B, C, NewA> groupKeyMappingA;
    private final TriFunction<A, B, C, NewB> groupKeyMappingB;

    public TriGroupBy2Map0CollectFastMutator(TriFunction<A, B, C, NewA> groupKeyMappingA,
            TriFunction<A, B, C, NewB> groupKeyMappingB) {
        this.groupKeyMappingA = groupKeyMappingA;
        this.groupKeyMappingB = groupKeyMappingB;
    }

    @Override
    public AbstractRuleAssembler apply(AbstractRuleAssembler ruleAssembler) {
        ruleAssembler.applyFilterToLastPrimaryPattern();
        Variable<A> inputA = ruleAssembler.getVariable(0);
        Variable<B> inputB = ruleAssembler.getVariable(1);
        Variable<C> inputC = ruleAssembler.getVariable(2);
        Variable<BiTuple<NewA, NewB>> groupKey = ruleAssembler.createVariable(BiTuple.class, "groupKey");
        ViewItem groupByPattern = groupBy(getInnerAccumulatePattern(ruleAssembler), inputA, inputB, inputC, groupKey,
                (a, b, c) -> new BiTuple<>(groupKeyMappingA.apply(a, b, c), groupKeyMappingB.apply(a, b, c)));
        List<ViewItem> newFinishedExpressions = new ArrayList<>(ruleAssembler.getFinishedExpressions());
        newFinishedExpressions.add(groupByPattern); // The last pattern is added here.
        Variable<NewA> newA = ruleAssembler.createVariable("newA", from(groupKey, k -> k.a));
        Variable<NewB> newB = ruleAssembler.createVariable("newB", from(groupKey, k -> k.b));
        PatternDSL.PatternDef<NewA> newAPattern = pattern(newA);
        newFinishedExpressions.add(newAPattern);
        PatternDSL.PatternDef<NewB> newPrimaryPattern = pattern(newB);
        return new BiRuleAssembler(ruleAssembler, ruleAssembler.getExpectedGroupByCount(), newFinishedExpressions,
                Arrays.asList(newA, newB), singletonList(newPrimaryPattern), emptyMap());
    }
}
