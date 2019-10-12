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

package org.optaplanner.core.impl.score.stream.drools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.drools.model.Global;
import org.drools.model.Rule;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.holder.AbstractScoreHolder;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.impl.score.stream.drools.common.DroolsAbstractConstraintStream;
import org.optaplanner.core.impl.score.stream.drools.uni.DroolsFromUniConstraintStream;

public class DroolsConstraint<Solution_> implements Constraint {

    private final DroolsConstraintFactory<Solution_> constraintFactory;
    private final String constraintPackage;
    private final String constraintName;
    private Function<Solution_, Score<?>> constraintWeightExtractor;
    private final boolean positive;
    private final List<DroolsFromUniConstraintStream<Solution_, Object>> fromStreamList;

    public DroolsConstraint(DroolsConstraintFactory<Solution_> constraintFactory,
            String constraintPackage, String constraintName,
            Function<Solution_, Score<?>> constraintWeightExtractor, boolean positive,
            List<DroolsFromUniConstraintStream<Solution_, Object>> fromStreamList) {
        this.constraintFactory = constraintFactory;
        this.constraintPackage = constraintPackage;
        this.constraintName = constraintName;
        this.constraintWeightExtractor = constraintWeightExtractor;
        this.positive = positive;
        this.fromStreamList = fromStreamList;
    }

    public Score<?> extractConstraintWeight(Solution_ workingSolution) {
        Score<?> constraintWeight = constraintWeightExtractor.apply(workingSolution);
        constraintFactory.getSolutionDescriptor().validateConstraintWeight(constraintPackage, constraintName, constraintWeight);
        return positive ? constraintWeight : constraintWeight.negate();
    }

    private Set<DroolsAbstractConstraintStream<Solution_>> assembleAllStreams() {
        final Set<DroolsAbstractConstraintStream<Solution_>> streams = new LinkedHashSet<>();
        for (DroolsAbstractConstraintStream<Solution_> fromStream: fromStreamList) {
            streams.addAll(assembleAllStreams(fromStream));
        }
        return streams;
    }

    private Collection<DroolsAbstractConstraintStream<Solution_>> assembleAllStreams(
            DroolsAbstractConstraintStream<Solution_> parent) {
        final Set<DroolsAbstractConstraintStream<Solution_>> streams = new LinkedHashSet<>();
        streams.add(parent);
        for (DroolsAbstractConstraintStream<Solution_> child: parent.getChildStreams()) {
            streams.addAll(assembleAllStreams(child));
        }
        return streams;
    }

    /**
     * Retrieves Drools rules required to process this constraint.
     *
     * @param ruleLibrary never null. Cache of rules already generated by previous constraints. This method will use
     * {@link Map#computeIfAbsent(Object, Function)} to add rules from {@link DroolsAbstractConstraintStream}s which we
     * have not yet processed. This way, we will only generate one rule per stream, allowing Drools to reuse the
     * computations.
     * @param scoreHolderGlobal never null. The Drools global used to track changes to score within rule consequences.
     * @return never null. List of all Drools {@link Rule}s required by this constraint. May update ruleLibrary as a
     * side effect.
     */
    public List<Rule> createRules(Map<DroolsAbstractConstraintStream<Solution_>, Rule> ruleLibrary,
            Global<? extends AbstractScoreHolder> scoreHolderGlobal) {
        Set<DroolsAbstractConstraintStream<Solution_>> streams = assembleAllStreams();
        List<Rule> rules = new ArrayList<>();
        streams.forEach(stream -> {
            Rule rule = ruleLibrary.computeIfAbsent(stream, key ->
                    key.buildRule(this, scoreHolderGlobal).orElse(null));
            if (rule != null) {
                rules.add(rule);
            }
        });
        int expectedRuleCount = rules.size();
        if (expectedRuleCount == 0) {
            throw new IllegalStateException("A constraint stream for constraint " + constraintPackage + " " +
                    constraintName + " resulted in no rules.");
        }
        return rules;
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public ConstraintFactory getConstraintFactory() {
        return constraintFactory;
    }

    @Override
    public String getConstraintPackage() {
        return constraintPackage;
    }

    @Override
    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public String toString() {
        return "DroolsConstraint(" + constraintPackage + " " + constraintName + ") in " + fromStreamList.size() +
                " from() stream(s)";
    }
}
