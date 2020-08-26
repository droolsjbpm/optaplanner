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
import java.util.function.BiFunction;

import org.drools.model.PatternDSL;
import org.drools.model.Variable;
import org.drools.model.view.ExprViewItem;
import org.drools.model.view.ViewItem;
import org.optaplanner.core.api.score.stream.bi.BiConstraintCollector;
import org.optaplanner.core.impl.score.stream.drools.bi.DroolsBiAccumulateFunction;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.drools.model.DSL.accFunction;
import static org.drools.model.PatternDSL.pattern;

class BiGroupBy1Map1CollectFastMutator<A, B, NewA, NewB> extends AbstractBiGroupByMutator {

    private final BiFunction<A, B, NewA> groupKeyMappingA;
    private final BiConstraintCollector<A, B, ?, NewB> collectorB;

    public BiGroupBy1Map1CollectFastMutator(BiFunction<A, B, NewA> groupKeyMappingA,
            BiConstraintCollector<A, B, ?, NewB> collectorB) {
        this.groupKeyMappingA = groupKeyMappingA;
        this.collectorB = collectorB;
    }

    @Override
    public AbstractRuleAssembler apply(AbstractRuleAssembler ruleAssembler) {
        ruleAssembler.applyFilterToLastPrimaryPattern();
        Variable<A> inputA = ruleAssembler.getVariable(0);
        Variable<B> inputB = ruleAssembler.getVariable(1);
        Variable<NewA> groupKey = (Variable<NewA>) PatternDSL.declarationOf(Object.class);
        Variable<NewB> output = (Variable<NewB>) PatternDSL.declarationOf(Object.class);
        DroolsBiAccumulateFunction<A, B, ?, NewB> bridge = new DroolsBiAccumulateFunction<>(collectorB);
        ExprViewItem<NewB> accumulatePattern = PatternDSL.groupBy(getInnerAccumulatePattern(ruleAssembler), inputA,
                inputB, groupKey, groupKeyMappingA::apply, accFunction(() -> bridge).as(output));
        List<ViewItem> newFinishedExpressions = new ArrayList<>(ruleAssembler.getFinishedExpressions());
        newFinishedExpressions.add(accumulatePattern); // The last pattern is added here.
        PatternDSL.PatternDef<NewB> newPrimaryPattern = pattern(output);
        return new BiRuleAssembler(ruleAssembler::generateNextId, ruleAssembler.getExpectedGroupByCount(),
                newFinishedExpressions, Arrays.asList(groupKey, output), singletonList(newPrimaryPattern), emptyMap());
    }
}
