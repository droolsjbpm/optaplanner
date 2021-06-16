/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.examples.taskassigning_listvariable.domain.solver;

import java.util.Objects;

import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.examples.taskassigning_listvariable.domain.Employee;
import org.optaplanner.examples.taskassigning_listvariable.domain.Task;
import org.optaplanner.examples.taskassigning_listvariable.domain.TaskAssigningSolution;

public class CustomCollectionInverseRelationShadowVariableListener
        implements VariableListener<TaskAssigningSolution, Employee> {

    @Override
    public void beforeEntityAdded(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        updateEmployeeAndIndex(scoreDirector, employee);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        updateEmployeeAndIndex(scoreDirector, employee);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        // Do nothing
    }

    private void updateEmployeeAndIndex(ScoreDirector<TaskAssigningSolution> scoreDirector, Employee employee) {
        if (employee.getTasks() == null) {
            return;
        }
        int index = 0;
        for (Task task : employee.getTasks()) {
            if (!Objects.equals(task.getEmployee(), employee)) {
                scoreDirector.beforeVariableChanged(task, "employee");
                task.setEmployee(employee);
                scoreDirector.afterVariableChanged(task, "employee");
            }
            if (!Objects.equals(task.getIndex(), index)) {
                scoreDirector.beforeVariableChanged(task, "index");
                task.setIndex(index);
                scoreDirector.afterVariableChanged(task, "index");
            }
            index++;
        }
    }
}
