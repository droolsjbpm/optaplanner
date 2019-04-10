/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.solver;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.event.BestSolutionListener;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.optaplanner.core.api.solver.event.SolverListener;
import org.optaplanner.core.impl.partitionedsearch.PartitionSolver;
import org.optaplanner.core.impl.phase.Phase;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleListener;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleSupport;
import org.optaplanner.core.impl.phase.scope.AbstractPhaseScope;
import org.optaplanner.core.impl.phase.scope.AbstractStepScope;
import org.optaplanner.core.impl.solver.event.BestSolutionListenerSupport;
import org.optaplanner.core.impl.solver.event.DeprecatedSolverEventListenerSupport;
import org.optaplanner.core.impl.solver.event.SolverListenerSupport;
import org.optaplanner.core.impl.solver.recaller.BestSolutionRecaller;
import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;
import org.optaplanner.core.impl.solver.termination.Termination;

/**
 * Common code between {@link DefaultSolver} and child solvers (such as {@link PartitionSolver}.
 * <p>
 * Do not create a new child {@link Solver} to implement a new heuristic or metaheuristic,
 * just use a new {@link Phase} for that.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Solver
 * @see DefaultSolver
 */
public abstract class AbstractSolver<Solution_> implements Solver<Solution_> {

    protected final SolverListenerSupport<Solution_> solverListenerSupport = new SolverListenerSupport<>(this);
    protected final BestSolutionListenerSupport<Solution_> bestSolutionListenerSupport = new BestSolutionListenerSupport<>(this);
    protected final DeprecatedSolverEventListenerSupport<Solution_> deprecatedSolverEventListenerSupport = new DeprecatedSolverEventListenerSupport<>(this);
    protected final PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    protected final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    // Note that the DefaultSolver.basicPlumbingTermination is a component of this termination
    protected final Termination termination;
    protected final List<Phase<Solution_>> phaseList;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public AbstractSolver(BestSolutionRecaller<Solution_> bestSolutionRecaller, Termination termination,
            List<Phase<Solution_>> phaseList) {
        this.bestSolutionRecaller = bestSolutionRecaller;
        this.termination = termination;
        bestSolutionRecaller.setBestSolutionListenerSupport(bestSolutionListenerSupport);
        bestSolutionRecaller.setDeprecatedSolverEventListenerSupport(deprecatedSolverEventListenerSupport);
        this.phaseList = phaseList;
        for (Phase<Solution_> phase : phaseList) {
            phase.setSolverPhaseLifecycleSupport(phaseLifecycleSupport);
        }
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void solvingStarted(DefaultSolverScope<Solution_> solverScope) {
        solverListenerSupport.fireSolvingStarted(solverScope);
        solverScope.setWorkingSolutionFromBestSolution();
        bestSolutionRecaller.solvingStarted(solverScope);
        termination.solvingStarted(solverScope);
        phaseLifecycleSupport.fireSolvingStarted(solverScope);
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingStarted(solverScope);
        }
    }

    protected void runPhases(DefaultSolverScope<Solution_> solverScope) {
        Iterator<Phase<Solution_>> it = phaseList.iterator();
        while (!termination.isSolverTerminated(solverScope) && it.hasNext()) {
            Phase<Solution_> phase = it.next();
            phase.solve(solverScope);
            if (it.hasNext()) {
                solverScope.setWorkingSolutionFromBestSolution();
            }
        }
        // TODO support doing round-robin of phases (only non-construction heuristics)
    }

    public void solvingEnded(DefaultSolverScope<Solution_> solverScope) {
        for (Phase<Solution_> phase : phaseList) {
            phase.solvingEnded(solverScope);
        }
        bestSolutionRecaller.solvingEnded(solverScope);
        termination.solvingEnded(solverScope);
        phaseLifecycleSupport.fireSolvingEnded(solverScope);
        solverScope.endingNow();
        solverListenerSupport.fireSolvingEnded(solverScope);
    }

    // ************************************************************************
    // Event listeners
    // ************************************************************************

    @Override
    public void addSolverListener(SolverListener<Solution_> listener) {
        if (isSolving()) {
            throw new ConcurrentModificationException("The solver is solving.");
        }
        solverListenerSupport.addEventListener(listener);
    }

    @Override
    public void removeSolverListener(SolverListener<Solution_> listener) {
        if (isSolving()) {
            throw new ConcurrentModificationException("The solver is solving.");
        }
        solverListenerSupport.removeEventListener(listener);
    }

    @Override
    public void addBestSolutionListener(BestSolutionListener<Solution_> listener) {
        if (isSolving()) {
            throw new ConcurrentModificationException("The solver is solving.");
        }
        bestSolutionListenerSupport.addEventListener(listener);
    }

    @Override
    public void removeBestSolutionListener(BestSolutionListener<Solution_> listener) {
        if (isSolving()) {
            throw new ConcurrentModificationException("The solver is solving.");
        }
        bestSolutionListenerSupport.removeEventListener(listener);
    }

    @Override
    @Deprecated
    public void addEventListener(SolverEventListener<Solution_> listener) {
        if (isSolving()) {
            throw new ConcurrentModificationException("The solver is solving.");
        }
        deprecatedSolverEventListenerSupport.addEventListener(listener);
    }

    @Override
    @Deprecated
    public void removeEventListener(SolverEventListener<Solution_> listener) {
        if (isSolving()) {
            throw new ConcurrentModificationException("The solver is solving.");
        }
        deprecatedSolverEventListenerSupport.removeEventListener(listener);
    }

    /**
     * Add a {@link PhaseLifecycleListener} that is notified
     * of {@link PhaseLifecycleListener#solvingStarted(DefaultSolverScope)} solving} events
     * and also of the {@link PhaseLifecycleListener#phaseStarted(AbstractPhaseScope) phase}
     * and the {@link PhaseLifecycleListener#stepStarted(AbstractStepScope)} step} starting/ending events of all phases.
     * <p>
     * To get notified for only 1 phase, use {@link Phase#addPhaseLifecycleListener(PhaseLifecycleListener)} instead.
     * @param phaseLifecycleListener never null
     */
    public void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.addEventListener(phaseLifecycleListener);
    }

    /**
     * @param phaseLifecycleListener never null
     * @see #addPhaseLifecycleListener(PhaseLifecycleListener)
     */
    public void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.removeEventListener(phaseLifecycleListener);
    }

}
