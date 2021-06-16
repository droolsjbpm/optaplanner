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

package org.optaplanner.examples.taskassigning_listvariable.domain.solver.solution.initializer;

import java.util.ArrayList;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import org.optaplanner.examples.taskassigning_listvariable.domain.Employee;
import org.optaplanner.examples.taskassigning_listvariable.domain.Task;
import org.optaplanner.examples.taskassigning_listvariable.domain.TaskAssigningSolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TmpRoundRobinInitializer implements CustomPhaseCommand<TaskAssigningSolution> {

    private static final Logger logger = LoggerFactory.getLogger(TmpRoundRobinInitializer.class);

    @Override
    public void changeWorkingSolution(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        TaskAssigningSolution solution = scoreDirector.getWorkingSolution();

        solution.getEmployeeList().stream()
                .filter(employee -> employee.getTasks() == null)
                .forEach(employee -> employee.setTasks(new ArrayList<>()));

        int employeeListSize = solution.getEmployeeList().size();
        int tasksAssigned = 0;

        for (int i = 0; i < solution.getTaskList().size(); i++) {
            Task task = solution.getTaskList().get(i);
            if (task.getEmployee() == null) {
                Employee employee = solution.getEmployeeList().get(i % employeeListSize);
                scoreDirector.beforeVariableChanged(employee, "tasks");
                employee.getTasks().add(task);
                scoreDirector.afterVariableChanged(employee, "tasks");
                tasksAssigned++;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("    {} task{} ha{} been assigned.",
                    tasksAssigned,
                    tasksAssigned == 1 ? "" : "s",
                    tasksAssigned == 1 ? "s" : "ve");
        }

        scoreDirector.triggerVariableListeners();
    }
}
