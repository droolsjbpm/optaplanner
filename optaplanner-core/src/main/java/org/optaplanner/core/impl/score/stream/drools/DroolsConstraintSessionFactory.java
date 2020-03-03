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

package org.optaplanner.core.impl.score.stream.drools;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.drools.model.Model;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.builder.KieBaseBuilder;
import org.kie.api.KieBase;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieSession;
import org.kie.internal.event.rule.RuleEventManager;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.holder.AbstractScoreHolder;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirector;
import org.optaplanner.core.impl.score.director.drools.OptaPlannerRuleEventListener;
import org.optaplanner.core.impl.score.stream.ConstraintSession;
import org.optaplanner.core.impl.score.stream.ConstraintSessionFactory;
import org.optaplanner.core.impl.score.stream.drools.common.DroolsRuleStructure;
import org.optaplanner.core.impl.score.stream.drools.common.FactTuple;

import static java.util.stream.Collectors.toMap;

public class DroolsConstraintSessionFactory<Solution_> implements ConstraintSessionFactory<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Model originalModel;
    private KieBase originalKieBase;
    private KieBase activeKieBase;
    private Set<String> activeConstraintIdSet = null;
    private final Map<Rule, DroolsConstraint<Solution_>> compiledRuleToConstraintMap;
    private final Map<String, org.drools.model.Rule> constraintToModelRuleMap;

    public DroolsConstraintSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor, Model model,
            List<DroolsConstraint<Solution_>> constraintList) {
        this.solutionDescriptor = solutionDescriptor;
        this.originalModel = model;
        this.originalKieBase = KieBaseBuilder.createKieBaseFromModel(model);
        this.activeKieBase = originalKieBase;
        this.compiledRuleToConstraintMap = constraintList.stream()
                .collect(toMap(constraint -> activeKieBase.getRule(constraint.getConstraintPackage(),
                        constraint.getConstraintName()), Function.identity()));
        this.constraintToModelRuleMap = constraintList.stream()
                .collect(toMap(Constraint::getConstraintId, constraint -> model.getRules().stream()
                        .filter(rule -> Objects.equals(rule.getName(), constraint.getConstraintName()))
                        .filter(rule -> Objects.equals(rule.getPackage(), constraint.getConstraintPackage()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Programming error: Rule for constraint (" +
                                constraint + ") not found."))));
    }

    @Override
    public ConstraintSession<Solution_> buildSession(boolean constraintMatchEnabled, Solution_ workingSolution) {
        ScoreDefinition scoreDefinition = solutionDescriptor.getScoreDefinition();
        AbstractScoreHolder scoreHolder = (AbstractScoreHolder) scoreDefinition.buildScoreHolder(constraintMatchEnabled);
        scoreHolder.setJustificationListConverter((justificationList, rule) ->
                matchJustificationsToOutput((List<Object>) justificationList,
                        compiledRuleToConstraintMap.get(rule).getExpectedJustificationTypes()));
        // Determine which rules to enable based on the fact that their constraints carry weight.
        Score<?> zero = scoreDefinition.getZeroScore();
        Set<String> enabledConstraintIdSet = new LinkedHashSet<>(compiledRuleToConstraintMap.size());
        compiledRuleToConstraintMap.forEach((compiledRule, constraint) -> {
            Score<?> constraintWeight = constraint.extractConstraintWeight(workingSolution);
            scoreHolder.configureConstraintWeight(compiledRule, constraintWeight);
            if (!constraintWeight.equals(zero)) {
                enabledConstraintIdSet.add(constraint.getConstraintId());
            }
        });
        // Determine the KieBase to use.
        boolean allAreEnabled = enabledConstraintIdSet.size() == compiledRuleToConstraintMap.size();
        if (allAreEnabled) { // Shortcut; don't change the original KieBase.
            activeKieBase = originalKieBase;
            activeConstraintIdSet = null;
        } else if (!enabledConstraintIdSet.equals(activeConstraintIdSet)) {
            // Only rebuild the active KieBase when the set of enabled constraints changed.
            ModelImpl model = new ModelImpl().withGlobals(originalModel.getGlobals());
            enabledConstraintIdSet.forEach(constraintId -> model.addRule(constraintToModelRuleMap.get(constraintId)));
            activeKieBase = KieBaseBuilder.createKieBaseFromModel(model);
            activeConstraintIdSet = enabledConstraintIdSet;
        }
        // Create the session itself.
        KieSession kieSession = activeKieBase.newKieSession();
        ((RuleEventManager) kieSession).addEventListener(new OptaPlannerRuleEventListener()); // Enables undo in rules.
        kieSession.setGlobal(DroolsScoreDirector.GLOBAL_SCORE_HOLDER_KEY, scoreHolder);
        return new DroolsConstraintSession<>(kieSession, scoreHolder);
    }

    /**
     * Converts justification list to another justification list, this one matching the expected scoring stream.
     * For example, if a scoring stream of cardinality 2 operates on facts of A and B, the list returned by this
     * method will only have these two facts. Order is not guaranteed.
     *
     * <p>
     * Due to the nature of the justification list coming from Drools, this method is very fragile.
     * The facts often come unordered and mixed with other facts not relevant to the problem at hand.
     * Therefore, this method is a set of heuristics that makes all the constraint stream tests pass.
     * However, it is possible that, as new constraint stream building block combinations are tested, the set of
     * heuristics inside this method will have to be redesigned.
     *
     * @param justificationList unordered list of justifications coming from the score director
     * @param expectedTypes as defined by {@link DroolsRuleStructure#getExpectedJustificationTypes()}
     * @return never null
     */
    private static List<Object> matchJustificationsToOutput(List<Object> justificationList, Class... expectedTypes) {
        if (expectedTypes.length == 0) {
            throw new IllegalStateException("Impossible: there are no 0-cardinality constraint streams.");
        }
        Object[] matching = new Object[expectedTypes.length];
        // First process non-Object matches, as those are the most descriptive.
        for (int i = 0; i < expectedTypes.length; i++) {
            Class expectedType = expectedTypes[i];
            if (Objects.equals(expectedType, Object.class)) {
                continue;
            }
            Object match = justificationList.stream()
                    .filter(j -> expectedType.isAssignableFrom(j.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Impossible: no justification of type ("
                            + expectedType + ")."));
            justificationList.remove(match);
            matching[i] = match;
        }
        // Fill the remaining places with Object matches, but keep their original order coming from expectedMatches.
        for (int i = 0; i < expectedTypes.length; i++) {
            if (matching[i] != null) {
                continue;
            }
            Object match = justificationList.stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Impossible: there are no more constraint matches."));
            justificationList.remove(match);
            matching[i] = match;
        }
        if (matching.length > 1) {
            // The justifications will be enumerated. A, B, C, ...
            return Arrays.asList(matching);
        }
        Object item = matching[0];
        Class expectedType = expectedTypes[0];
        if (FactTuple.class.isAssignableFrom(expectedType)) {
            // The justifications will all come from a single tuple (eg. BiTuple<A, B>).
            return ((FactTuple) item).asList();
        } else {
            // This comes from a uni stream.
            return Collections.singletonList(item);
        }
    }

}
