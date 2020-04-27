/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.localsearch.decider.acceptor.lateacceptance;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchPhaseScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;
import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;
import org.optaplanner.core.impl.testdata.domain.TestdataSolution;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.*;

public class LateAcceptanceAcceptorTest extends AbstractAcceptorTest {

    @Test
    public void lateAcceptanceSize() {
        LateAcceptanceAcceptor acceptor = new LateAcceptanceAcceptor();
        acceptor.setLateAcceptanceSize(3);
        acceptor.setHillClimbingEnabled(false);

        DefaultSolverScope<TestdataSolution> solverScope = new DefaultSolverScope<>();
        solverScope.setBestScore(SimpleScore.of(-1000));
        LocalSearchPhaseScope<TestdataSolution> phaseScope = new LocalSearchPhaseScope<>(solverScope);
        LocalSearchStepScope<TestdataSolution> lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(SimpleScore.of(Integer.MIN_VALUE));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lateScore = -1000
        LocalSearchStepScope<TestdataSolution> stepScope0 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope0 = buildMoveScope(stepScope0, -500);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900)));
        assertEquals(true, acceptor.isAccepted(moveScope0));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -800)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope0, -2000)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -1000)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore(moveScope0.getScore());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lateScore = -1000
        LocalSearchStepScope<TestdataSolution> stepScope1 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope1 = buildMoveScope(stepScope1, -700);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope1, -900)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope1, -2000)));
        assertEquals(true, acceptor.isAccepted(moveScope1));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope1, -1000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope1, -1001)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lateScore = -1000
        LocalSearchStepScope<TestdataSolution> stepScope2 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope2 = buildMoveScope(stepScope1, -400);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope2, -900)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope2, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope2, -1001)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope2, -1000)));
        assertEquals(true, acceptor.isAccepted(moveScope2));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // lateScore = -500
        LocalSearchStepScope<TestdataSolution> stepScope3 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope3 = buildMoveScope(stepScope1, -200);
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope3, -900)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope3, -500)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope3, -501)));
        assertEquals(true, acceptor.isAccepted(moveScope3));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope3, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope3.setStep(moveScope3.getMove());
        stepScope3.setScore(moveScope3.getScore());
        solverScope.setBestScore(moveScope3.getScore());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // lateScore = -700 (not the best score of -500!)
        LocalSearchStepScope<TestdataSolution> stepScope4 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope4 = buildMoveScope(stepScope1, -300);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope4, -700)));
        assertEquals(true, acceptor.isAccepted(moveScope4));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope4, -500)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope4, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope4, -701)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -700))); // Repeated call
        stepScope4.setStep(moveScope4.getMove());
        stepScope4.setScore(moveScope4.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // lateScore = -400
        LocalSearchStepScope<TestdataSolution> stepScope5 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope5 = buildMoveScope(stepScope1, -300);
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -401)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope5, -400)));
        assertEquals(true, acceptor.isAccepted(moveScope5));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -600)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope0, -401))); // Repeated call
        stepScope5.setStep(moveScope5.getMove());
        stepScope5.setScore(moveScope5.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    public void hillClimbingEnabled() {
        LateAcceptanceAcceptor acceptor = new LateAcceptanceAcceptor();
        acceptor.setLateAcceptanceSize(2);
        acceptor.setHillClimbingEnabled(true);

        DefaultSolverScope<TestdataSolution> solverScope = new DefaultSolverScope<>();
        solverScope.setBestScore(SimpleScore.of(-1000));
        LocalSearchPhaseScope<TestdataSolution> phaseScope = new LocalSearchPhaseScope<>(solverScope);
        LocalSearchStepScope<TestdataSolution> lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lateScore = -1000, lastCompletedStepScore = Integer.MIN_VALUE
        LocalSearchStepScope<TestdataSolution> stepScope0 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope0 = buildMoveScope(stepScope0, -500);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900)));
        assertEquals(true, acceptor.isAccepted(moveScope0));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -800)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope0, -2000)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -1000)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore(moveScope0.getScore());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lateScore = -1000, lastCompletedStepScore = -500
        LocalSearchStepScope<TestdataSolution> stepScope1 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope1 = buildMoveScope(stepScope1, -700);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope1, -900)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope1, -2000)));
        assertEquals(true, acceptor.isAccepted(moveScope1));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope1, -1000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope1, -1001)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lateScore = -500, lastCompletedStepScore = -700
        LocalSearchStepScope<TestdataSolution> stepScope2 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope2 = buildMoveScope(stepScope1, -400);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope2, -700)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope2, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope2, -701)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope2, -600)));
        assertEquals(true, acceptor.isAccepted(moveScope2));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -700))); // Repeated call
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // lateScore = -700, lastCompletedStepScore = -400
        LocalSearchStepScope<TestdataSolution> stepScope3 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope3 = buildMoveScope(stepScope1, -200);
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope3, -900)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope3, -700)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope3, -701)));
        assertEquals(true, acceptor.isAccepted(moveScope3));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope3, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope0, -900))); // Repeated call
        stepScope3.setStep(moveScope3.getMove());
        stepScope3.setScore(moveScope3.getScore());
        solverScope.setBestScore(moveScope3.getScore());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // lateScore = -400 (not the best score of -200!), lastCompletedStepScore = -200
        LocalSearchStepScope<TestdataSolution> stepScope4 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope4 = buildMoveScope(stepScope1, -300);
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope4, -400)));
        assertEquals(true, acceptor.isAccepted(moveScope4));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope4, -500)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope4, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope4, -401)));
        assertEquals(true, acceptor.isAccepted(buildMoveScope(stepScope0, -400))); // Repeated call
        stepScope4.setStep(moveScope4.getMove());
        stepScope4.setScore(moveScope4.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // lateScore = -200, lastCompletedStepScore = -300
        LocalSearchStepScope<TestdataSolution> stepScope5 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope5 = buildMoveScope(stepScope1, -300);
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -301)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -400)));
        assertEquals(true, acceptor.isAccepted(moveScope5));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -2000)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope5, -600)));
        assertEquals(false, acceptor.isAccepted(buildMoveScope(stepScope0, -301))); // Repeated call
        stepScope5.setStep(moveScope5.getMove());
        stepScope5.setScore(moveScope5.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    public void zeroLateAcceptanceSize() {
        LateAcceptanceAcceptor acceptor = new LateAcceptanceAcceptor();
        acceptor.setLateAcceptanceSize(0);
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }

    @Test
    public void negativeLateAcceptanceSize() {
        LateAcceptanceAcceptor acceptor = new LateAcceptanceAcceptor();
        acceptor.setLateAcceptanceSize(-1);
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }
}
