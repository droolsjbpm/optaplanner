/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.score.director.drools;

import java.util.Collection;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.holder.ScoreHolder;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.VariableDescriptor;
import org.optaplanner.core.impl.score.director.AbstractScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirector;

/**
 * Drools implementation of {@link ScoreDirector}, which directs the Rule Engine to calculate the {@link Score}
 * of the {@link PlanningSolution working solution}.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see ScoreDirector
 */
public class DroolsScoreDirector<Solution_>
        extends AbstractScoreDirector<Solution_, DroolsScoreDirectorFactory<Solution_>> {

    public static final String GLOBAL_SCORE_HOLDER_KEY = "scoreHolder";

    protected KieSession kieSession;
    protected ScoreHolder workingScoreHolder;
    private DroolsReproducer reproducer = new DroolsReproducer();

    public DroolsScoreDirector(DroolsScoreDirectorFactory<Solution_> scoreDirectorFactory,
                               boolean constraintMatchEnabledPreference) {
        super(scoreDirectorFactory, constraintMatchEnabledPreference);
        reproducer.setDomainPackage(scoreDirectorFactory.getSolutionDescriptor().getSolutionClass().getPackage().getName());
    }

    public KieSession getKieSession() {
        return kieSession;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {
        super.setWorkingSolution(workingSolution);
        resetKieSession();
    }

    private void resetKieSession() {
        if (kieSession != null) {
            reproducer.dispose();
            kieSession.dispose();
        }
        kieSession = scoreDirectorFactory.newKieSession();
        workingScoreHolder = getScoreDefinition().buildScoreHolder(constraintMatchEnabledPreference);
        kieSession.setGlobal(GLOBAL_SCORE_HOLDER_KEY, workingScoreHolder);
        // TODO Adjust when uninitialized entities from getWorkingFacts get added automatically too (and call afterEntityAdded)
        Collection<Object> workingFacts = getWorkingFacts();
        reproducer.addFacts(workingFacts);
        for (Object fact : workingFacts) {
            reproducer.insert(fact);
            kieSession.insert(fact);
        }
    }

    public Collection<Object> getWorkingFacts() {
        return getSolutionDescriptor().getAllFacts(workingSolution);
    }

    @Override
    public Score calculateScore() {
        variableListenerSupport.assertNotificationQueuesAreEmpty();
        try {
            reproducer.fireAllRules();
            kieSession.fireAllRules();
        } catch (RuntimeException e) {
            logger.error("kieSession.fireAllRules() failed:", e);
            logger.info("Starting replay & reduce to find a minimal Drools reproducer.");
            reproducer.replay(kieSession);
            throw new IllegalStateException("Reproducer should have failed!");
        }
        Score score = workingScoreHolder.extractScore(workingInitScore);
        setCalculatedScore(score);
        return score;
    }

    @Override
    public boolean isConstraintMatchEnabled() {
        return workingScoreHolder.isConstraintMatchEnabled();
    }

    @Override
    public Collection<ConstraintMatchTotal> getConstraintMatchTotals() {
        if (workingSolution == null) {
            throw new IllegalStateException(
                    "The method setWorkingSolution() must be called before the method getConstraintMatchTotals().");
        }
        reproducer.fireAllRules();
        kieSession.fireAllRules();
        return workingScoreHolder.getConstraintMatchTotals();
    }

    @Override
    public DroolsScoreDirector<Solution_> clone() {
        // TODO experiment with serializing the KieSession to clone it and its entities but not its other facts.
        // See drools-compiler's test SerializationHelper.getSerialisedStatefulKnowledgeSession(...)
        // and use an identity FactFactory that:
        // - returns the reference for a non-@PlanningEntity fact
        // - returns a clone for a @PlanningEntity fact (Pitfall: chained planning entities)
        // Note: currently that will break incremental score calculation, but future drools versions might fix that
        return (DroolsScoreDirector<Solution_>) super.clone();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (kieSession != null) {
            reproducer.dispose();
            kieSession.dispose();
            kieSession = null;
        }
    }

    // ************************************************************************
    // Entity/variable add/change/remove methods
    // ************************************************************************

    // public void beforeEntityAdded(EntityDescriptor entityDescriptor, Object entity) // Do nothing

    @Override
    public void afterEntityAdded(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("The entity (" + entity + ") cannot be added to the ScoreDirector.");
        }
        if (!getSolutionDescriptor().hasEntityDescriptor(entity.getClass())) {
            throw new IllegalArgumentException("The entity (" + entity + ") of class (" + entity.getClass()
                    + ") is not a configured @PlanningEntity.");
        }
        if (kieSession.getFactHandle(entity) != null) {
            throw new IllegalArgumentException("The entity (" + entity
                    + ") was already added to this ScoreDirector."
                    + " Usually the cause is that that specific instance was already in your Solution's entities" +
                    " and you probably want to use before/afterVariableChanged() instead.");
        }
        reproducer.insert(entity);
        kieSession.insert(entity);
        super.afterEntityAdded(entityDescriptor, entity);
    }

    // public void beforeVariableChanged(VariableDescriptor variableDescriptor, Object entity) // Do nothing

    @Override
    public void afterVariableChanged(VariableDescriptor variableDescriptor, Object entity) {
        reproducer.update(entity, variableDescriptor);
        update(entity);
        super.afterVariableChanged(variableDescriptor, entity);
    }

    private void update(Object entity) {
        FactHandle factHandle = kieSession.getFactHandle(entity);
        if (factHandle == null) {
            throw new IllegalArgumentException("The entity (" + entity
                    + ") was never added to this ScoreDirector.\n"
                    + "Maybe that specific instance is not in the return values of the "
                    + PlanningSolution.class.getSimpleName() + "'s entity members ("
                    + getSolutionDescriptor().getEntityMemberAndEntityCollectionMemberNames() + ").");
        }
        kieSession.update(factHandle, entity);
    }

    // public void beforeEntityRemoved(EntityDescriptor entityDescriptor, Object entity) // Do nothing

    @Override
    public void afterEntityRemoved(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        FactHandle factHandle = kieSession.getFactHandle(entity);
        if (factHandle == null) {
            throw new IllegalArgumentException("The entity (" + entity
                    + ") was never added to this ScoreDirector.\n"
                    + "Maybe that specific instance is not in the return values of the "
                    + PlanningSolution.class.getSimpleName() + "'s entity members ("
                    + getSolutionDescriptor().getEntityMemberAndEntityCollectionMemberNames() + ").");
        }
        reproducer.delete(entity);
        kieSession.delete(factHandle);
        super.afterEntityRemoved(entityDescriptor, entity);
    }


    // ************************************************************************
    // Problem fact add/change/remove methods
    // ************************************************************************

    // public void beforeProblemFactAdded(Object problemFact) // Do nothing

    @Override
    public void afterProblemFactAdded(Object problemFact) {
        if (kieSession.getFactHandle(problemFact) != null) {
            throw new IllegalArgumentException("The problemFact (" + problemFact
                    + ") was already added to this ScoreDirector.\n"
                    + "Maybe that specific instance is already in the "
                    + PlanningSolution.class.getSimpleName() + "'s problem fact members ("
                    + getSolutionDescriptor().getProblemFactMemberAndProblemFactCollectionMemberNames() + ").\n"
                    + "Maybe use before/afterProblemPropertyChanged() instead of before/afterProblemFactAdded().");
        }
        reproducer.insert(problemFact);
        kieSession.insert(problemFact);
        super.afterProblemFactAdded(problemFact);
    }

    // public void beforeProblemPropertyChanged(Object problemFactOrEntity) // Do nothing

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {
        FactHandle factHandle = kieSession.getFactHandle(problemFactOrEntity);
        if (factHandle == null) {
            throw new IllegalArgumentException("The problemFact (" + problemFactOrEntity
                    + ") was never added to this ScoreDirector.\n"
                    + "Maybe that specific instance is not in the "
                    + PlanningSolution.class.getSimpleName() + "'s problem fact members ("
                    + getSolutionDescriptor().getProblemFactMemberAndProblemFactCollectionMemberNames() + ").");
        }
        kieSession.update(factHandle, problemFactOrEntity);
        super.afterProblemPropertyChanged(problemFactOrEntity);
    }

    // public void beforeProblemFactRemoved(Object problemFact) // Do nothing

    @Override
    public void afterProblemFactRemoved(Object problemFact) {
        FactHandle factHandle = kieSession.getFactHandle(problemFact);
        if (factHandle == null) {
            throw new IllegalArgumentException("The problemFact (" + problemFact
                    + ") was never added to this ScoreDirector.\n"
                    + "Maybe that specific instance is not in the "
                    + PlanningSolution.class.getSimpleName() + "'s problem fact members ("
                    + getSolutionDescriptor().getProblemFactMemberAndProblemFactCollectionMemberNames() + ").");
        }
        reproducer.delete(problemFact);
        kieSession.delete(factHandle);
        super.afterProblemFactRemoved(problemFact);
    }

}
