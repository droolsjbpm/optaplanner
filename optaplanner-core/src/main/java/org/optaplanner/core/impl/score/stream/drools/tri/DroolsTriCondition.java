/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.score.stream.drools.tri;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.drools.model.Drools;
import org.drools.model.Global;
import org.drools.model.PatternDSL;
import org.drools.model.RuleItemBuilder;
import org.drools.model.Variable;
import org.drools.model.consequences.ConsequenceBuilder;
import org.drools.model.functions.Block5;
import org.drools.model.functions.Predicate3;
import org.kie.api.runtime.rule.RuleContext;
import org.optaplanner.core.api.function.ToIntTriFunction;
import org.optaplanner.core.api.function.ToLongTriFunction;
import org.optaplanner.core.api.function.TriFunction;
import org.optaplanner.core.api.function.TriPredicate;
import org.optaplanner.core.api.score.holder.AbstractScoreHolder;

import static org.drools.model.DSL.on;

public final class DroolsTriCondition<A, B, C> {

    private final DroolsTriRuleStructure<A, B, C> ruleStructure;

    public DroolsTriCondition(DroolsTriRuleStructure<A, B, C> ruleStructure) {
        this.ruleStructure = ruleStructure;
    }

    public DroolsTriRuleStructure<A, B, C> getRuleStructure() {
        return ruleStructure;
    }

    public DroolsTriCondition<A, B, C> andFilter(TriPredicate<A, B, C> predicate) {
        Predicate3<Object, A, B> filter = (c, a, b) -> predicate.test(a, b, (C) c);
        Variable<A> aVariable = ruleStructure.getA();
        Variable<B> bVariable = ruleStructure.getB();
        Variable<C> cVariable = ruleStructure.getC();
        Supplier<PatternDSL.PatternDef<?>> newTargetPattern = () -> ruleStructure.getTargetPattern()
                .expr("Filter using " + predicate, aVariable, bVariable, filter);
        DroolsTriRuleStructure<A, B, C> newRuleStructure = new DroolsTriRuleStructure<>(aVariable, bVariable, cVariable,
                newTargetPattern, ruleStructure.getSupportingRuleItems());
        return new DroolsTriCondition<>(newRuleStructure);
    }

    public List<RuleItemBuilder<?>> completeWithScoring(Global<? extends AbstractScoreHolder<?>> scoreHolderGlobal) {
        return completeWithScoring(scoreHolderGlobal, (drools, scoreHolder, __, ___, ____) -> {
            RuleContext kcontext = (RuleContext) drools;
            scoreHolder.impactScore(kcontext);
        });
    }

    public List<RuleItemBuilder<?>> completeWithScoring(Global<? extends AbstractScoreHolder<?>> scoreHolderGlobal,
            ToIntTriFunction<A, B, C> matchWeighter) {
        return completeWithScoring(scoreHolderGlobal, (drools, scoreHolder, a, b, c) -> {
            RuleContext kcontext = (RuleContext) drools;
            scoreHolder.impactScore(kcontext, matchWeighter.applyAsInt(a, b, c));
        });
    }

    public List<RuleItemBuilder<?>> completeWithScoring(Global<? extends AbstractScoreHolder<?>> scoreHolderGlobal,
            ToLongTriFunction<A, B, C> matchWeighter) {
        return completeWithScoring(scoreHolderGlobal, (drools, scoreHolder, a, b, c) -> {
            RuleContext kcontext = (RuleContext) drools;
            scoreHolder.impactScore(kcontext, matchWeighter.applyAsLong(a, b, c));
        });
    }

    public List<RuleItemBuilder<?>> completeWithScoring(
            Global<? extends AbstractScoreHolder<?>> scoreHolderGlobal,
            TriFunction<A, B, C, BigDecimal> matchWeighter) {
        return completeWithScoring(scoreHolderGlobal, (drools, scoreHolder, a, b, c) -> {
            RuleContext kcontext = (RuleContext) drools;
            scoreHolder.impactScore(kcontext, matchWeighter.apply(a, b, c));
        });
    }

    private <ScoreHolder extends AbstractScoreHolder<?>> List<RuleItemBuilder<?>> completeWithScoring(
            Global<ScoreHolder> scoreHolderGlobal,
            Block5<Drools, ScoreHolder, A, B, C> consequenceImpl) {
        ConsequenceBuilder._4<ScoreHolder, A, B, C> consequence =
                on(scoreHolderGlobal, ruleStructure.getA(), ruleStructure.getB(), ruleStructure.getC())
                        .execute(consequenceImpl);
        return rebuildRuleItems(ruleStructure, ruleStructure.getTargetPattern(), consequence);

    }

    private List<RuleItemBuilder<?>> rebuildRuleItems(DroolsTriRuleStructure<A, B, C> ruleStructure,
            RuleItemBuilder<?>... toAdd) {
        List<RuleItemBuilder<?>> supporting = new ArrayList<>(ruleStructure.getSupportingRuleItems());
        for (RuleItemBuilder<?> ruleItem : toAdd) {
            supporting.add(ruleItem);
        }
        return supporting;
    }


}
