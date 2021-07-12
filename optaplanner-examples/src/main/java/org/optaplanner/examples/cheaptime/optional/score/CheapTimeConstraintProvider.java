/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.examples.cheaptime.optional.score;

import java.util.function.Function;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.examples.cheaptime.domain.Machine;
import org.optaplanner.examples.cheaptime.domain.Period;
import org.optaplanner.examples.cheaptime.domain.Resource;
import org.optaplanner.examples.cheaptime.domain.TaskAssignment;
import org.optaplanner.examples.common.experimental.api.ConsecutiveIntervalInfo;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;
import static org.optaplanner.core.api.score.stream.Joiners.filtering;
import static org.optaplanner.core.api.score.stream.Joiners.greaterThan;
import static org.optaplanner.core.api.score.stream.Joiners.greaterThanOrEqual;
import static org.optaplanner.core.api.score.stream.Joiners.lessThan;
import static org.optaplanner.examples.cheaptime.score.CheapTimeCostCalculator.multiplyTwoMicros;
import static org.optaplanner.examples.cheaptime.score.CheapTimeIncrementalScoreCalculator.CONSTRAINT_PACKAGE;
import static org.optaplanner.examples.common.experimental.ExperimentalConstraintCollectors.consecutiveIntervals;

public class CheapTimeConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                startTimeLimitsFrom(constraintFactory),
                startTimeLimitsTo(constraintFactory),
                maximumCapacity(constraintFactory),
                activeMachinePowerCost(constraintFactory),
                activeMachineSpinUpAndDownCost(constraintFactory),
                idleCosts(constraintFactory),
                taskPowerCost(constraintFactory),
                startEarly(constraintFactory)
        };
    }

    protected Constraint startTimeLimitsFrom(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> taskAssignment.getStartPeriod() < taskAssignment.getTask().getStartPeriodRangeFrom())
                .penalizeLong(CONSTRAINT_PACKAGE, "Task starts too early", HardMediumSoftLongScore.ONE_HARD,
                        taskAssignment -> taskAssignment.getStartPeriod() - taskAssignment.getTask().getStartPeriodRangeFrom());
    }

    protected Constraint startTimeLimitsTo(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .filter(taskAssignment -> taskAssignment.getStartPeriod() >= taskAssignment.getTask().getStartPeriodRangeTo())
                .penalizeLong(CONSTRAINT_PACKAGE, "Task starts too late", HardMediumSoftLongScore.ONE_HARD,
                        taskAssignment -> taskAssignment.getTask().getStartPeriodRangeTo() - taskAssignment.getStartPeriod());
    }

    protected Constraint maximumCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Period.class)
                .join(TaskAssignment.class,
                        greaterThanOrEqual(Period::getPeriod, TaskAssignment::getStartPeriod),
                        lessThan(Period::getPeriod, TaskAssignment::getEndPeriod))
                .join(Resource.class,
                        filtering((period, taskAssignment, resource) -> taskAssignment.getTask().getUsage(resource) > 0))
                .groupBy((period, taskAssignment, resource) -> period,
                        (period, taskAssignment, resource) -> resource,
                        (period, taskAssignment, resource) -> taskAssignment.getMachine(),
                        sum((period, taskAssignment, resource) -> taskAssignment.getTask().getUsage(resource)))
                .filter((period, resource, machine, usage) -> machine.getMachineCapacity(resource).getCapacity() < usage)
                .penalizeLong(CONSTRAINT_PACKAGE, "Maximum resource capacity", HardMediumSoftLongScore.ONE_HARD,
                        (period, resource, machine, usage) -> usage - machine.getMachineCapacity(resource).getCapacity());
    }

    protected Constraint activeMachinePowerCost(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Period.class)
                .join(Machine.class)
                .ifExists(TaskAssignment.class,
                        Joiners.equal((period, machine) -> machine, TaskAssignment::getMachine),
                        greaterThanOrEqual((period, machine) -> period.getPeriod(), TaskAssignment::getStartPeriod),
                        lessThan((period, machine) -> period.getPeriod(), TaskAssignment::getEndPeriod))
                .penalizeLong(CONSTRAINT_PACKAGE, "Active machine power cost", HardMediumSoftLongScore.ONE_MEDIUM,
                        (period, machine) -> multiplyTwoMicros(machine.getPowerConsumptionMicros(),
                                period.getPowerPriceMicros()));
    }

    protected Constraint activeMachineSpinUpAndDownCost(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Machine.class)
                .ifExists(TaskAssignment.class,
                        Joiners.equal(Function.identity(), TaskAssignment::getMachine))
                .penalizeLong(CONSTRAINT_PACKAGE, "Active machine spin up and down cost", HardMediumSoftLongScore.ONE_MEDIUM,
                        machine -> machine.getSpinUpDownCostMicros() * 2);
    }

    protected Constraint idleCosts(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .groupBy(TaskAssignment::getMachine,
                        consecutiveIntervals(TaskAssignment::getStartPeriod, TaskAssignment::getEndPeriod, (a, b) -> b - a))
                .flattenLast(ConsecutiveIntervalInfo::getBreaks) // add break
                .join(Period.class,
                        greaterThan((machine, brk) -> brk.getPreviousIntervalClusterEnd(), Period::getPeriod),
                        lessThan((machine, brk) -> brk.getNextIntervalClusterStart(), Period::getPeriod))
                .groupBy((machine, idlePeriod, period) -> machine,
                        (machine, idlePeriod, period) -> idlePeriod,
                        ConstraintCollectors.sumLong((machine, idlePeriod, period) -> period.getPowerPriceMicros()))
                .penalizeLong(CONSTRAINT_PACKAGE, "Machine idle costs", HardMediumSoftLongScore.ONE_MEDIUM,
                        (machine, idlePeriod, idlePowerCost) -> {
                            long idleCost = multiplyTwoMicros(machine.getPowerConsumptionMicros(), idlePowerCost);
                            // Shutting down and restarting the machine may be cheaper than keeping it idle.
                            return Math.min(idleCost, machine.getSpinUpDownCostMicros() * 2);
                        });
    }

    protected Constraint taskPowerCost(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Period.class)
                .join(TaskAssignment.class,
                        greaterThanOrEqual(Period::getPeriod, TaskAssignment::getStartPeriod),
                        lessThan(Period::getPeriod, TaskAssignment::getEndPeriod))
                .penalizeLong(CONSTRAINT_PACKAGE, "Task power cost", HardMediumSoftLongScore.ONE_MEDIUM,
                        (period, taskAssignment) -> multiplyTwoMicros(taskAssignment.getTask().getPowerConsumptionMicros(),
                                period.getPowerPriceMicros()));
    }

    protected Constraint startEarly(ConstraintFactory constraintFactory) {
        return constraintFactory.from(TaskAssignment.class)
                .penalize(CONSTRAINT_PACKAGE, "Prefer early task start", HardMediumSoftLongScore.ONE_SOFT, TaskAssignment::getStartPeriod);
    }

}
