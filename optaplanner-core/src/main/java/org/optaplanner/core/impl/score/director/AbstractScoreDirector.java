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

package org.optaplanner.core.impl.score.director;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.domain.common.accessor.MemberAccessor;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.lookup.ClassAndPlanningIdComparator;
import org.optaplanner.core.impl.domain.lookup.LookUpManager;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.VariableDescriptor;
import org.optaplanner.core.impl.domain.variable.listener.support.VariableListenerSupport;
import org.optaplanner.core.impl.domain.variable.supply.SupplyManager;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.solver.thread.ChildThreadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link ScoreDirector}.
 * <p>
 * Implementation note: Extending classes should follow these guidelines:
 * <ul>
 * <li>before* method: last statement should be a call to the super method</li>
 * <li>after* method: first statement should be a call to the super method</li>
 * </ul>
 *
 * @see ScoreDirector
 */
public abstract class AbstractScoreDirector<Solution_, Score_ extends Score<Score_>, Factory_ extends AbstractScoreDirectorFactory<Solution_, Score_>>
        implements InnerScoreDirector<Solution_, Score_>, Cloneable {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Class, MemberAccessor> planningIdAccessorCacheMap = new HashMap<>(0);
    protected final Factory_ scoreDirectorFactory;
    protected final boolean lookUpEnabled;
    protected final LookUpManager lookUpManager;
    protected boolean constraintMatchEnabledPreference;
    protected final VariableListenerSupport<Solution_> variableListenerSupport;

    protected Solution_ workingSolution;
    protected long workingEntityListRevision = 0L;
    protected Integer workingInitScore = null;

    protected boolean allChangesWillBeUndoneBeforeStepEnds = false;

    protected long calculationCount = 0L;

    protected AbstractScoreDirector(Factory_ scoreDirectorFactory,
            boolean lookUpEnabled, boolean constraintMatchEnabledPreference) {
        this.scoreDirectorFactory = scoreDirectorFactory;
        this.lookUpEnabled = lookUpEnabled;
        lookUpManager = lookUpEnabled
                ? new LookUpManager(scoreDirectorFactory.getSolutionDescriptor().getLookUpStrategyResolver())
                : null;
        this.constraintMatchEnabledPreference = constraintMatchEnabledPreference;
        variableListenerSupport = new VariableListenerSupport<>(this);
        variableListenerSupport.linkVariableListeners();
    }

    @Override
    public Factory_ getScoreDirectorFactory() {
        return scoreDirectorFactory;
    }

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return scoreDirectorFactory.getSolutionDescriptor();
    }

    @Override
    public ScoreDefinition<Score_> getScoreDefinition() {
        return scoreDirectorFactory.getScoreDefinition();
    }

    public boolean isLookUpEnabled() {
        return lookUpEnabled;
    }

    public boolean isConstraintMatchEnabledPreference() {
        return constraintMatchEnabledPreference;
    }

    @Override
    public void overwriteConstraintMatchEnabledPreference(boolean constraintMatchEnabledPreference) {
        this.constraintMatchEnabledPreference = constraintMatchEnabledPreference;
    }

    @Override
    public Solution_ getWorkingSolution() {
        return workingSolution;
    }

    @Override
    public long getWorkingEntityListRevision() {
        return workingEntityListRevision;
    }

    public boolean isAllChangesWillBeUndoneBeforeStepEnds() {
        return allChangesWillBeUndoneBeforeStepEnds;
    }

    @Override
    public void setAllChangesWillBeUndoneBeforeStepEnds(boolean allChangesWillBeUndoneBeforeStepEnds) {
        this.allChangesWillBeUndoneBeforeStepEnds = allChangesWillBeUndoneBeforeStepEnds;
    }

    @Override
    public long getCalculationCount() {
        return calculationCount;
    }

    @Override
    public void resetCalculationCount() {
        this.calculationCount = 0L;
    }

    @Override
    public SupplyManager getSupplyManager() {
        return variableListenerSupport;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        this.workingSolution = requireNonNull(workingSolution);
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        workingInitScore = -solutionDescriptor.countUninitializedVariables(workingSolution);
        Collection<Object> allFacts = solutionDescriptor.getAllFacts(workingSolution);
        if (lookUpEnabled) {
            lookUpManager.resetWorkingObjects(allFacts);
        }
        assertNonNullPlanningIds(allFacts);
        variableListenerSupport.resetWorkingSolution();
        setWorkingEntityListDirty();
    }

    @Override
    public void assertNonNullPlanningIds() {
        assertNonNullPlanningIds(getSolutionDescriptor().getAllFacts(workingSolution));
    }

    private void assertNonNullPlanningIds(Collection<Object> allFacts) {
        for (Object fact : allFacts) {
            Class factClass = fact.getClass();
            // Cannot use Map.computeIfAbsent(), as we also want to cache null values.
            if (!planningIdAccessorCacheMap.containsKey(factClass)) {
                planningIdAccessorCacheMap.put(factClass,
                        ConfigUtils.findPlanningIdMemberAccessor(factClass, getSolutionDescriptor().getDomainAccessType()));
            }
            MemberAccessor planningIdAccessor = planningIdAccessorCacheMap.get(factClass);
            if (planningIdAccessor == null) { // There is no planning ID annotation.
                continue;
            }
            Object id = planningIdAccessor.executeGetter(fact);
            if (id == null) { // Fail fast as planning ID is null.
                throw new IllegalStateException("The planningId (" + id + ") of the member (" + planningIdAccessor
                        + ") of the class (" + factClass + ") on object (" + fact + ") must not be null.\n"
                        + "Maybe initialize the planningId of the class (" + planningIdAccessor.getDeclaringClass()
                        + ") instance (" + fact + ") before solving.\n" +
                        "Maybe remove the " + PlanningId.class.getSimpleName() + " annotation.");
            }
        }
    }

    @Override
    public Score_ doAndProcessMove(Move<Solution_> move, boolean assertMoveScoreFromScratch) {
        Move<Solution_> undoMove = move.doMove(this);
        Score_ score = calculateScore();
        if (assertMoveScoreFromScratch) {
            assertWorkingScoreFromScratch(score, move);
        }
        undoMove.doMove(this);
        return score;
    }

    @Override
    public void doAndProcessMove(Move<Solution_> move, boolean assertMoveScoreFromScratch, Consumer<Score_> moveProcessor) {
        Move<Solution_> undoMove = move.doMove(this);
        Score_ score = calculateScore();
        if (assertMoveScoreFromScratch) {
            assertWorkingScoreFromScratch(score, move);
        }
        moveProcessor.accept(score);
        undoMove.doMove(this);
    }

    @Override
    public boolean isWorkingEntityListDirty(long expectedWorkingEntityListRevision) {
        return workingEntityListRevision != expectedWorkingEntityListRevision;
    }

    protected void setWorkingEntityListDirty() {
        workingEntityListRevision++;
    }

    @Override
    public Solution_ cloneWorkingSolution() {
        return cloneSolution(workingSolution);
    }

    @Override
    public Solution_ cloneSolution(Solution_ originalSolution) {
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        Score_ originalScore = (Score_) solutionDescriptor.getScore(originalSolution);
        Solution_ cloneSolution = solutionDescriptor.getSolutionCloner().cloneSolution(originalSolution);
        Score_ cloneScore = (Score_) solutionDescriptor.getScore(cloneSolution);
        if (scoreDirectorFactory.isAssertClonedSolution()) {
            if (!Objects.equals(originalScore, cloneScore)) {
                throw new IllegalStateException("Cloning corruption: "
                        + "the original's score (" + originalScore
                        + ") is different from the clone's score (" + cloneScore + ").\n"
                        + "Check the " + SolutionCloner.class.getSimpleName() + ".");
            }
            List<Object> originalEntityList = solutionDescriptor.getEntityList(originalSolution);
            Map<Object, Object> originalEntityMap = new IdentityHashMap<>(originalEntityList.size());
            for (Object originalEntity : originalEntityList) {
                originalEntityMap.put(originalEntity, null);
            }
            for (Object cloneEntity : solutionDescriptor.getEntityList(cloneSolution)) {
                if (originalEntityMap.containsKey(cloneEntity)) {
                    throw new IllegalStateException("Cloning corruption: "
                            + "the same entity (" + cloneEntity
                            + ") is present in both the original and the clone.\n"
                            + "So when a planning variable in the original solution changes, "
                            + "the cloned solution will change too.\n"
                            + "Check the " + SolutionCloner.class.getSimpleName() + ".");
                }
            }
        }
        return cloneSolution;
    }

    @Override
    public int getWorkingEntityCount() {
        return getSolutionDescriptor().getEntityCount(workingSolution);
    }

    @Override
    public List<Object> getWorkingEntityList() {
        return getSolutionDescriptor().getEntityList(workingSolution);
    }

    @Override
    public int getWorkingValueCount() {
        return getSolutionDescriptor().getValueCount(workingSolution);
    }

    @Override
    public void triggerVariableListeners() {
        variableListenerSupport.triggerVariableListenersInNotificationQueues();
    }

    protected void setCalculatedScore(Score_ score) {
        getSolutionDescriptor().setScore(workingSolution, score);
        calculationCount++;
    }

    @Override
    public AbstractScoreDirector<Solution_, Score_, Factory_> clone() {
        // Breaks incremental score calculation.
        // Subclasses should overwrite this method to avoid breaking it if possible.
        AbstractScoreDirector<Solution_, Score_, Factory_> clone =
                (AbstractScoreDirector<Solution_, Score_, Factory_>) scoreDirectorFactory
                        .buildScoreDirector(isLookUpEnabled(), constraintMatchEnabledPreference);
        clone.setWorkingSolution(cloneWorkingSolution());
        return clone;
    }

    @Override
    public InnerScoreDirector<Solution_, Score_> createChildThreadScoreDirector(ChildThreadType childThreadType) {
        if (childThreadType == ChildThreadType.PART_THREAD) {
            AbstractScoreDirector<Solution_, Score_, Factory_> childThreadScoreDirector =
                    (AbstractScoreDirector<Solution_, Score_, Factory_>) scoreDirectorFactory
                            .buildScoreDirector(isLookUpEnabled(), constraintMatchEnabledPreference);
            // ScoreCalculationCountTermination takes into account previous phases
            // but the calculationCount of partitions is maxed, not summed.
            childThreadScoreDirector.calculationCount = calculationCount;
            return childThreadScoreDirector;
        } else if (childThreadType == ChildThreadType.MOVE_THREAD) {
            // TODO The move thread must use constraintMatchEnabledPreference in FULL_ASSERT,
            // but it doesn't have to for Indictment Local Search, in which case it is a performance loss
            AbstractScoreDirector<Solution_, Score_, Factory_> childThreadScoreDirector =
                    (AbstractScoreDirector<Solution_, Score_, Factory_>) scoreDirectorFactory
                            .buildScoreDirector(true, constraintMatchEnabledPreference);
            childThreadScoreDirector.setWorkingSolution(cloneWorkingSolution());
            return childThreadScoreDirector;
        } else {
            throw new IllegalStateException("The childThreadType (" + childThreadType + ") is not implemented.");
        }
    }

    @Override
    public void close() {
        workingSolution = null;
        workingInitScore = null;
        if (lookUpEnabled) {
            lookUpManager.clearWorkingObjects();
        }
        variableListenerSupport.clearWorkingSolution();
    }

    // ************************************************************************
    // Entity/variable add/change/remove methods
    // ************************************************************************

    @Override
    public final void beforeEntityAdded(Object entity) {
        beforeEntityAdded(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    @Override
    public final void afterEntityAdded(Object entity) {
        afterEntityAdded(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    @Override
    public final void beforeVariableChanged(Object entity, String variableName) {
        VariableDescriptor<Solution_> variableDescriptor = getSolutionDescriptor()
                .findVariableDescriptorOrFail(entity, variableName);
        beforeVariableChanged(variableDescriptor, entity);
    }

    @Override
    public final void afterVariableChanged(Object entity, String variableName) {
        VariableDescriptor<Solution_> variableDescriptor = getSolutionDescriptor()
                .findVariableDescriptorOrFail(entity, variableName);
        afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public final void beforeEntityRemoved(Object entity) {
        beforeEntityRemoved(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    @Override
    public final void afterEntityRemoved(Object entity) {
        afterEntityRemoved(getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass()), entity);
    }

    public void beforeEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        variableListenerSupport.beforeEntityAdded(entityDescriptor, entity);
    }

    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        workingInitScore -= entityDescriptor.countUninitializedVariables(entity);
        if (lookUpEnabled) {
            lookUpManager.addWorkingObject(entity);
        }
        variableListenerSupport.afterEntityAdded(entityDescriptor, entity);
        if (!allChangesWillBeUndoneBeforeStepEnds) {
            setWorkingEntityListDirty();
        }
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (variableDescriptor.isGenuineAndUninitialized(entity)) {
            workingInitScore++;
        }
        variableListenerSupport.beforeVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (variableDescriptor.isGenuineAndUninitialized(entity)) {
            workingInitScore--;
        }
        variableListenerSupport.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void changeVariableFacade(VariableDescriptor<Solution_> variableDescriptor, Object entity, Object newValue) {
        beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        afterVariableChanged(variableDescriptor, entity);
    }

    public void beforeEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        workingInitScore += entityDescriptor.countUninitializedVariables(entity);
        variableListenerSupport.beforeEntityRemoved(entityDescriptor, entity);
    }

    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        if (lookUpEnabled) {
            lookUpManager.removeWorkingObject(entity);
        }
        variableListenerSupport.afterEntityRemoved(entityDescriptor, entity);
        if (!allChangesWillBeUndoneBeforeStepEnds) {
            setWorkingEntityListDirty();
        }
    }

    // ************************************************************************
    // Problem fact add/change/remove methods
    // ************************************************************************

    @Override
    public void beforeProblemFactAdded(Object problemFact) {
        // Do nothing
    }

    @Override
    public void afterProblemFactAdded(Object problemFact) {
        if (lookUpEnabled) {
            lookUpManager.addWorkingObject(problemFact);
        }
        variableListenerSupport.resetWorkingSolution(); // TODO do not nuke the variable listeners
    }

    @Override
    public void beforeProblemPropertyChanged(Object problemFactOrEntity) {
        // Do nothing
    }

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        variableListenerSupport.resetWorkingSolution(); // TODO do not nuke the variable listeners
    }

    @Override
    public void beforeProblemFactRemoved(Object problemFact) {
        // Do nothing
    }

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        if (lookUpEnabled) {
            lookUpManager.removeWorkingObject(problemFact);
        }
        variableListenerSupport.resetWorkingSolution(); // TODO do not nuke the variable listeners
    }

    @Override
    public <E> E lookUpWorkingObject(E externalObject) {
        if (!lookUpEnabled) {
            throw new IllegalStateException("When lookUpEnabled (" + lookUpEnabled
                    + ") is disabled in the constructor, this method should not be called.");
        }
        return lookUpManager.lookUpWorkingObject(externalObject);
    }

    @Override
    public <E> E lookUpWorkingObjectOrReturnNull(E externalObject) {
        if (!lookUpEnabled) {
            throw new IllegalStateException("When lookUpEnabled (" + lookUpEnabled
                    + ") is disabled in the constructor, this method should not be called.");
        }
        return lookUpManager.lookUpWorkingObjectOrReturnNull(externalObject);
    }

    // ************************************************************************
    // Assert methods
    // ************************************************************************

    @Override
    public void assertExpectedWorkingScore(Score_ expectedWorkingScore, Object completedAction) {
        Score_ workingScore = calculateScore();
        if (!expectedWorkingScore.equals(workingScore)) {
            throw new IllegalStateException(
                    "Score corruption (" + expectedWorkingScore.subtract(workingScore).toShortString()
                            + "): the expectedWorkingScore (" + expectedWorkingScore
                            + ") is not the workingScore (" + workingScore
                            + ") after completedAction (" + completedAction + ").");
        }
    }

    @Override
    public void assertShadowVariablesAreNotStale(Score_ expectedWorkingScore, Object completedAction) {
        String violationMessage = createShadowVariablesViolationMessage();
        if (violationMessage != null) {
            throw new IllegalStateException(
                    VariableListener.class.getSimpleName() + " corruption after completedAction ("
                            + completedAction + "):\n"
                            + violationMessage);
        }
        Score_ workingScore = calculateScore();
        if (!expectedWorkingScore.equals(workingScore)) {
            assertWorkingScoreFromScratch(workingScore,
                    "assertShadowVariablesAreNotStale(" + expectedWorkingScore + ", " + completedAction + ")");
            throw new IllegalStateException("Impossible " + VariableListener.class.getSimpleName() + " corruption ("
                    + expectedWorkingScore.subtract(workingScore).toShortString() + "):"
                    + " the expectedWorkingScore (" + expectedWorkingScore
                    + ") is not the workingScore (" + workingScore
                    + ") after all " + VariableListener.class.getSimpleName()
                    + "s were triggered without changes to the genuine variables"
                    + " after completedAction (" + completedAction + ").\n"
                    + "But all the shadow variable values are still the same, so this is impossible.\n"
                    + "Maybe run with " + EnvironmentMode.FULL_ASSERT + " if you aren't already, to fail earlier.");
        }
    }

    /**
     * @param predicted true if the score was predicted and might have been calculated on another thread
     * @return never null
     */
    protected String buildShadowVariableAnalysis(boolean predicted) {
        String violationMessage = createShadowVariablesViolationMessage();
        String workingLabel = predicted ? "working" : "corrupted";
        if (violationMessage == null) {
            return "Shadow variable corruption in the " + workingLabel + " scoreDirector:\n"
                    + "  None";
        }
        return "Shadow variable corruption in the " + workingLabel + " scoreDirector:\n"
                + violationMessage
                + "  Maybe there is a bug in the VariableListener of those shadow variable(s).";
    }

    /**
     * @return null if there are no violations
     */
    protected String createShadowVariablesViolationMessage() {
        Map<ShadowVariableDescriptor<Solution_>, List<String>> violationListMap = new TreeMap<>(
                comparing(ShadowVariableDescriptor::getGlobalShadowOrder));
        SolutionDescriptor<Solution_> solutionDescriptor = getSolutionDescriptor();
        Map<Object, Map<ShadowVariableDescriptor<Solution_>, Object>> entityToShadowVariableValuesMap = new IdentityHashMap<>();
        for (Iterator<Object> it = solutionDescriptor.extractAllEntitiesIterator(workingSolution); it.hasNext();) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            Collection<ShadowVariableDescriptor<Solution_>> shadowVariableDescriptors = entityDescriptor
                    .getShadowVariableDescriptors();
            Map<ShadowVariableDescriptor<Solution_>, Object> shadowVariableValuesMap =
                    new HashMap<>(shadowVariableDescriptors.size());
            for (ShadowVariableDescriptor<Solution_> shadowVariableDescriptor : shadowVariableDescriptors) {
                Object value = shadowVariableDescriptor.getValue(entity);
                shadowVariableValuesMap.put(shadowVariableDescriptor, value);
            }
            entityToShadowVariableValuesMap.put(entity, shadowVariableValuesMap);
        }
        variableListenerSupport.triggerAllVariableListeners();
        for (Iterator<Object> it = solutionDescriptor.extractAllEntitiesIterator(workingSolution); it.hasNext();) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            Collection<ShadowVariableDescriptor<Solution_>> shadowVariableDescriptors = entityDescriptor
                    .getShadowVariableDescriptors();
            Map<ShadowVariableDescriptor<Solution_>, Object> shadowVariableValuesMap =
                    entityToShadowVariableValuesMap.get(entity);
            for (ShadowVariableDescriptor<Solution_> shadowVariableDescriptor : shadowVariableDescriptors) {
                Object newValue = shadowVariableDescriptor.getValue(entity);
                Object originalValue = shadowVariableValuesMap.get(shadowVariableDescriptor);
                if (!Objects.equals(originalValue, newValue)) {
                    List<String> violationList = violationListMap.computeIfAbsent(shadowVariableDescriptor,
                            k -> new ArrayList<>());
                    violationList.add("    The entity (" + entity
                            + ")'s shadow variable (" + shadowVariableDescriptor.getSimpleEntityAndVariableName()
                            + ")'s corrupted value (" + originalValue + ") changed to uncorrupted value (" + newValue
                            + ") after all " + VariableListener.class.getSimpleName()
                            + "s were triggered without changes to the genuine variables.\n"
                            + "      Maybe the " + VariableListener.class.getSimpleName() + " class ("
                            + shadowVariableDescriptor.getVariableListenerClass().getSimpleName()
                            + ") for that shadow variable (" + shadowVariableDescriptor.getSimpleEntityAndVariableName()
                            + ") forgot to update it when one of its sources changed.\n");
                }
            }
        }
        if (violationListMap.isEmpty()) {
            return null;
        }
        final int SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT = 3;
        StringBuilder message = new StringBuilder();
        violationListMap.forEach((shadowVariableDescriptor, violationList) -> {
            violationList.stream().limit(SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT).forEach(message::append);
            if (violationList.size() >= SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT) {
                message.append("  ... ").append(violationList.size() - SHADOW_VARIABLE_VIOLATION_DISPLAY_LIMIT)
                        .append(" more\n");
            }
        });
        return message.toString();
    }

    @Override
    public void assertWorkingScoreFromScratch(Score_ workingScore, Object completedAction) {
        assertScoreFromScratch(workingScore, completedAction, false);
    }

    @Override
    public void assertPredictedScoreFromScratch(Score_ workingScore, Object completedAction) {
        assertScoreFromScratch(workingScore, completedAction, true);
    }

    private void assertScoreFromScratch(Score_ score, Object completedAction, boolean predicted) {
        InnerScoreDirectorFactory<Solution_, Score_> assertionScoreDirectorFactory = scoreDirectorFactory
                .getAssertionScoreDirectorFactory();
        if (assertionScoreDirectorFactory == null) {
            assertionScoreDirectorFactory = scoreDirectorFactory;
        }
        try (InnerScoreDirector<Solution_, Score_> uncorruptedScoreDirector =
                assertionScoreDirectorFactory.buildScoreDirector(false,
                        true)) {
            uncorruptedScoreDirector.setWorkingSolution(workingSolution);
            Score_ uncorruptedScore = uncorruptedScoreDirector.calculateScore();
            if (!score.equals(uncorruptedScore)) {
                String scoreCorruptionAnalysis = buildScoreCorruptionAnalysis(uncorruptedScoreDirector, predicted);
                String shadowVariableAnalysis = buildShadowVariableAnalysis(predicted);
                throw new IllegalStateException(
                        "Score corruption (" + score.subtract(uncorruptedScore).toShortString()
                                + "): the " + (predicted ? "predictedScore" : "workingScore") + " (" + score
                                + ") is not the uncorruptedScore (" + uncorruptedScore
                                + ") after completedAction (" + completedAction + "):\n"
                                + scoreCorruptionAnalysis + "\n"
                                + shadowVariableAnalysis);
            }
        }
    }

    @Override
    public void assertExpectedUndoMoveScore(Move<Solution_> move, Score_ beforeMoveScore) {
        Score_ undoScore = calculateScore();
        if (!undoScore.equals(beforeMoveScore)) {
            logger.trace("        Corruption detected. Diagnosing...");
            // TODO PLANNER-421 Avoid undoMove.toString() because it's stale (because the move is already done)
            String undoMoveString = "Undo(" + move + ")";
            // Precondition: assert that there are probably no corrupted constraints
            assertWorkingScoreFromScratch(undoScore, undoMoveString);
            // Precondition: assert that shadow variables aren't stale after doing the undoMove
            assertShadowVariablesAreNotStale(undoScore, undoMoveString);
            String scoreDifference = undoScore.subtract(beforeMoveScore).toShortString();
            throw new IllegalStateException("UndoMove corruption (" + scoreDifference
                    + "): the beforeMoveScore (" + beforeMoveScore + ") is not the undoScore (" + undoScore
                    + ") which is the uncorruptedScore (" + undoScore + ") of the workingSolution.\n"
                    + "  1) Enable EnvironmentMode " + EnvironmentMode.FULL_ASSERT
                    + " (if you haven't already) to fail-faster in case there's a score corruption or variable listener corruption.\n"
                    + "  2) Check the Move.createUndoMove(...) method of the moveClass (" + move.getClass() + ")."
                    + " The move (" + move + ") might have a corrupted undoMove (" + undoMoveString + ").\n"
                    + "  3) Check your custom " + VariableListener.class.getSimpleName() + "s (if you have any)"
                    + " for shadow variables that are used by score constraints that could cause"
                    + " the scoreDifference (" + scoreDifference + ").");
        }
    }

    /**
     * @param uncorruptedScoreDirector never null
     * @param predicted true if the score was predicted and might have been calculated on another thread
     * @return never null
     */
    protected String buildScoreCorruptionAnalysis(InnerScoreDirector<Solution_, Score_> uncorruptedScoreDirector,
            boolean predicted) {
        if (!isConstraintMatchEnabled() || !uncorruptedScoreDirector.isConstraintMatchEnabled()) {
            return "Score corruption analysis could not be generated because"
                    + " either corrupted constraintMatchEnabled (" + isConstraintMatchEnabled()
                    + ") or uncorrupted constraintMatchEnabled (" + uncorruptedScoreDirector.isConstraintMatchEnabled()
                    + ") is disabled.\n"
                    + "  Check your score constraints manually.";
        }

        Map<String, ConstraintMatchTotal<Score_>> constraintMatchTotalMap = getConstraintMatchTotalMap();
        Map<List<Object>, ConstraintMatch<Score_>> corruptedMap =
                createConstraintMatchMap(constraintMatchTotalMap.values());
        Map<List<Object>, ConstraintMatch<Score_>> excessMap = new LinkedHashMap<>(corruptedMap);
        Map<String, ConstraintMatchTotal<Score_>> uncorruptedConstraintMatchTotalMap =
                uncorruptedScoreDirector.getConstraintMatchTotalMap();
        Map<List<Object>, ConstraintMatch<Score_>> missingMap =
                createConstraintMatchMap(uncorruptedConstraintMatchTotalMap.values());
        excessMap.keySet().removeAll(missingMap.keySet()); // missingMap == uncorruptedMap
        missingMap.keySet().removeAll(corruptedMap.keySet());

        final int CONSTRAINT_MATCH_DISPLAY_LIMIT = 8;
        StringBuilder analysis = new StringBuilder();
        analysis.append("Score corruption analysis:\n");
        // If predicted, the score calculation might have happened on another thread, so a different ScoreDirector
        // so there is no guarantee that the working ScoreDirector is the corrupted ScoreDirector
        String workingLabel = predicted ? "working" : "corrupted";
        if (excessMap.isEmpty()) {
            analysis.append("  The ").append(workingLabel)
                    .append(" scoreDirector has no ConstraintMatch(s) which are in excess.\n");
        } else {
            analysis.append("  The ").append(workingLabel).append(" scoreDirector has ").append(excessMap.size())
                    .append(" ConstraintMatch(s) which are in excess (and should not be there):\n");
            excessMap.values().stream().sorted().limit(CONSTRAINT_MATCH_DISPLAY_LIMIT)
                    .forEach(constraintMatch -> analysis.append("    ").append(constraintMatch).append("\n"));
            if (excessMap.size() >= CONSTRAINT_MATCH_DISPLAY_LIMIT) {
                analysis.append("    ... ").append(excessMap.size() - CONSTRAINT_MATCH_DISPLAY_LIMIT)
                        .append(" more\n");
            }
        }
        if (missingMap.isEmpty()) {
            analysis.append("  The ").append(workingLabel)
                    .append(" scoreDirector has no ConstraintMatch(s) which are missing.\n");
        } else {
            analysis.append("  The ").append(workingLabel).append(" scoreDirector has ").append(missingMap.size())
                    .append(" ConstraintMatch(s) which are missing:\n");
            missingMap.values().stream().sorted().limit(CONSTRAINT_MATCH_DISPLAY_LIMIT)
                    .forEach(constraintMatch -> analysis.append("    ").append(constraintMatch).append("\n"));
            if (missingMap.size() >= CONSTRAINT_MATCH_DISPLAY_LIMIT) {
                analysis.append("    ... ").append(missingMap.size() - CONSTRAINT_MATCH_DISPLAY_LIMIT)
                        .append(" more\n");
            }
        }
        if (!excessMap.isEmpty() || !missingMap.isEmpty()) {
            analysis.append("  Maybe there is a bug in the score constraints of those ConstraintMatch(s).\n");
            analysis.append(
                    "  Maybe a score constraint doesn't select all the entities it depends on, but finds some through a reference in a selected entity."
                            + " This corrupts incremental score calculation, because the constraint is not re-evaluated if such a non-selected entity changes.");
        } else {
            if (predicted) {
                analysis.append("  If multithreaded solving is active,"
                        + " the working scoreDirector is probably not the corrupted scoreDirector.\n");
                analysis.append("  If multithreaded solving is active, maybe the rebase() method of the move is bugged.\n");
                analysis.append("  If multithreaded solving is active,"
                        + " maybe a VariableListener affected the moveThread's workingSolution after doing and undoing a move,"
                        + " but this didn't happen here on the solverThread, so we can't detect it.");
            } else {
                analysis.append("  Impossible state. Maybe this is a bug in the scoreDirector (").append(getClass())
                        .append(").");
            }
        }
        return analysis.toString();
    }

    private Map<List<Object>, ConstraintMatch<Score_>> createConstraintMatchMap(
            Collection<ConstraintMatchTotal<Score_>> constraintMatchTotals) {
        Comparator<Object> comparator = new ClassAndPlanningIdComparator(getSolutionDescriptor().getDomainAccessType(),
                false);
        Map<List<Object>, ConstraintMatch<Score_>> constraintMatchMap = new LinkedHashMap<>(constraintMatchTotals.size() * 16);
        for (ConstraintMatchTotal<Score_> constraintMatchTotal : constraintMatchTotals) {
            for (ConstraintMatch<Score_> constraintMatch : constraintMatchTotal.getConstraintMatchSet()) {
                // The order of justificationLists for constraints that include accumulates isn't stable, so we make it.
                List<Object> justificationList = new ArrayList<>(constraintMatch.getJustificationList());
                justificationList.sort(comparator);
                // And now we store the reference to the constraint match.
                List<Object> key = Arrays.asList(constraintMatchTotal.getConstraintPackage(),
                        constraintMatchTotal.getConstraintName(), justificationList, constraintMatch.getScore());
                ConstraintMatch<Score_> previousConstraintMatch = constraintMatchMap.put(key, constraintMatch);
                if (previousConstraintMatch != null) {
                    throw new IllegalStateException("Score corruption because the constraintMatch (" + constraintMatch
                            + ") was added twice for constraintMatchTotal (" + constraintMatchTotal
                            + ") without removal.");
                }
            }
        }
        return constraintMatchMap;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + calculationCount + ")";
    }

}
