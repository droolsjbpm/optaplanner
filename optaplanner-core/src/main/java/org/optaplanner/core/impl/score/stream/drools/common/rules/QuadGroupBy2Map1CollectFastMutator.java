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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.drools.model.PatternDSL;
import org.drools.model.Variable;
import org.drools.model.view.ExprViewItem;
import org.drools.model.view.ViewItem;
import org.optaplanner.core.api.function.QuadFunction;
import org.optaplanner.core.api.score.stream.quad.QuadConstraintCollector;
import org.optaplanner.core.impl.score.stream.drools.common.BiTuple;
import org.optaplanner.core.impl.score.stream.drools.quad.DroolsQuadAccumulateFunction;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.drools.model.DSL.accFunction;
import static org.drools.model.PatternDSL.from;
import static org.drools.model.PatternDSL.groupBy;
import static org.drools.model.PatternDSL.pattern;

final class QuadGroupBy2Map1CollectFastMutator<A, B, C, D, NewA, NewB, NewC> extends AbstractQuadGroupByMutator {

    private final QuadFunction<A, B, C, D, NewA> groupKeyMappingA;
    private final QuadFunction<A, B, C, D, NewB> groupKeyMappingB;
    private final QuadConstraintCollector<A, B, C, D, ?, NewC> collectorC;

    public QuadGroupBy2Map1CollectFastMutator(QuadFunction<A, B, C, D, NewA> groupKeyMappingA,
            QuadFunction<A, B, C, D, NewB> groupKeyMappingB, QuadConstraintCollector<A, B, C, D, ?, NewC> collectorC) {
        this.groupKeyMappingA = groupKeyMappingA;
        this.groupKeyMappingB = groupKeyMappingB;
        this.collectorC = collectorC;
    }

    @Override
    public AbstractRuleAssembler apply(AbstractRuleAssembler ruleAssembler) {
        ruleAssembler.applyFilterToLastPrimaryPattern();
        Variable<A> inputA = ruleAssembler.getVariable(0);
        Variable<B> inputB = ruleAssembler.getVariable(1);
        Variable<C> inputC = ruleAssembler.getVariable(2);
        Variable<D> inputD = ruleAssembler.getVariable(3);
        Variable<BiTuple<NewA, NewB>> groupKey =
                (Variable<BiTuple<NewA, NewB>>) ruleAssembler.createVariable(BiTuple.class, "groupKey");
        Variable<NewC> output = ruleAssembler.createVariable("output");
        ExprViewItem accumulatePattern = groupBy(getInnerAccumulatePattern(ruleAssembler), inputA, inputB, inputC,
                inputD, groupKey, (a, b, c, d) -> new BiTuple<>(groupKeyMappingA.apply(a, b, c, d),
                        groupKeyMappingB.apply(a, b, c, d)),
                accFunction(() -> new DroolsQuadAccumulateFunction<>(collectorC)).as(output));
        List<ViewItem> newFinishedExpressions = new ArrayList<>(ruleAssembler.getFinishedExpressions());
        newFinishedExpressions.add(accumulatePattern); // The last pattern is added here.
        Variable<NewA> newA = ruleAssembler.createVariable("newA", from(groupKey, k -> k.a));
        Variable<NewB> newB = ruleAssembler.createVariable("newB", from(groupKey, k -> k.b));
        PatternDSL.PatternDef<NewA> newAPattern = pattern(newA);
        newFinishedExpressions.add(newAPattern);
        PatternDSL.PatternDef<NewB> newBPattern = pattern(newB);
        newFinishedExpressions.add(newBPattern);
        PatternDSL.PatternDef<NewC> newPrimaryPattern = pattern(output);
        return new TriRuleAssembler(ruleAssembler, ruleAssembler.getExpectedGroupByCount(),
                newFinishedExpressions, Arrays.asList(newA, newB, output), singletonList(newPrimaryPattern),
                emptyMap());
    }
}
