/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.taskassigning.domain.solver.move;

import java.util.List;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.examples.taskassigning.domain.Employee;
import org.optaplanner.examples.taskassigning.domain.Task;
import org.optaplanner.examples.taskassigning.domain.TaskAssigningSolution;

public class ListRemoveItemMove extends AbstractMove<TaskAssigningSolution> {

    private final Employee employee;
    private final Task task;

    public ListRemoveItemMove(Employee employee, Task task) {
        this.employee = employee;
        this.task = task;
    }

    @Override
    protected AbstractMove<TaskAssigningSolution> createUndoMove(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        return new ListAppendMove(employee, task);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        if (!employee.getTasks().contains(task)) {
            throw new IllegalStateException(
                    "Cannot remote task (" + task + ") from employee's (" + employee + ") tasks (" + task + ").");
        }
        employee.getTasks().remove(task);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        List<Task> tasks = scoreDirector.lookUpWorkingObject(employee).getTasks();
        return tasks.indexOf(task) == tasks.size() - 1;
    }

    @Override
    public String toString() {
        return employee + ": " + employee.getTasks() + "-" + task;
    }
}
