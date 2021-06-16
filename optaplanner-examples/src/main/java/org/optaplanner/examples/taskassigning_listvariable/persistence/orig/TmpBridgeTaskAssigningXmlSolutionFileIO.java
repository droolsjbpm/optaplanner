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

package org.optaplanner.examples.taskassigning_listvariable.persistence.orig;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.optaplanner.examples.common.domain.AbstractPersistable;
import org.optaplanner.examples.taskassigning_listvariable.domain.Affinity;
import org.optaplanner.examples.taskassigning_listvariable.domain.Customer;
import org.optaplanner.examples.taskassigning_listvariable.domain.Employee;
import org.optaplanner.examples.taskassigning_listvariable.domain.Priority;
import org.optaplanner.examples.taskassigning_listvariable.domain.Skill;
import org.optaplanner.examples.taskassigning_listvariable.domain.Task;
import org.optaplanner.examples.taskassigning_listvariable.domain.TaskAssigningSolution;
import org.optaplanner.examples.taskassigning_listvariable.domain.TaskType;
import org.optaplanner.examples.taskassigning_listvariable.persistence.TaskAssigningXmlSolutionFileIO;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a solution that was serialized from the original domain model on master into the new non-chained model.
 */
public class TmpBridgeTaskAssigningXmlSolutionFileIO implements SolutionFileIO<TaskAssigningSolution> {

    private static final Logger logger = LoggerFactory.getLogger(TmpBridgeTaskAssigningXmlSolutionFileIO.class);

    private final org.optaplanner.examples.taskassigning.persistence.TaskAssigningXmlSolutionFileIO origIO =
            new org.optaplanner.examples.taskassigning.persistence.TaskAssigningXmlSolutionFileIO();
    private final TaskAssigningXmlSolutionFileIO currentIO = new TaskAssigningXmlSolutionFileIO();

    @Override
    public TaskAssigningSolution read(File inputSolutionFile) {
        if (inputSolutionFile.getParentFile().getParentFile().getName().equals("taskassigning")) {
            logger.info("Converting original task assigning data to the LIST VARIABLE domain.");
            return convert(origIO.read(inputSolutionFile));
        }
        return currentIO.read(inputSolutionFile);
    }

    private TaskAssigningSolution convert(org.optaplanner.examples.taskassigning.domain.TaskAssigningSolution origSolution) {
        Map<Long, Skill> skillByIdMap = origSolution.getSkillList().stream()
                .map(origSkill -> new Skill(
                        origSkill.getId(),
                        origSkill.getName()))
                .collect(Collectors.toMap(AbstractPersistable::getId, Function.identity()));
        Map<Long, TaskType> taskTypeByIdMap = origSolution.getTaskTypeList().stream()
                .map(origTaskType -> {
                    TaskType taskType = new TaskType(
                            origTaskType.getId(),
                            origTaskType.getCode(),
                            origTaskType.getTitle(),
                            origTaskType.getBaseDuration());
                    taskType.setRequiredSkillList(
                            origTaskType.getRequiredSkillList().stream()
                                    .map(AbstractPersistable::getId)
                                    .map(skillByIdMap::get)
                                    .collect(Collectors.toList()));
                    return taskType;
                })
                .collect(Collectors.toMap(AbstractPersistable::getId, Function.identity()));
        Map<Long, Customer> customerByIdMap = origSolution.getCustomerList().stream()
                .map(customer -> new Customer(
                        customer.getId(),
                        customer.getName()))
                .collect(Collectors.toMap(AbstractPersistable::getId, Function.identity()));
        Map<Long, Employee> employeeByIdMap = origSolution.getEmployeeList().stream()
                .map(origEmployee -> {
                    Employee employee = new Employee(
                            origEmployee.getId(),
                            origEmployee.getFullName());
                    employee.setSkillSet(origEmployee.getSkillSet().stream()
                            .map(AbstractPersistable::getId)
                            .map(skillByIdMap::get)
                            .collect(Collectors.toSet()));
                    employee.setAffinityMap(origEmployee.getAffinityMap().entrySet().stream()
                            .map(customerAffinityEntry -> Pair.of(customerByIdMap.get(customerAffinityEntry.getKey().getId()),
                                    Affinity.valueOf(customerAffinityEntry.getValue().name())))
                            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
                    // Tasks will be added later.
                    return employee;
                })
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        Map<Long, Task> taskByIdMap = origSolution.getTaskList().stream()
                .map(origTask -> {
                    Task task = new Task(
                            origTask.getId(),
                            taskTypeByIdMap.get(origTask.getTaskType().getId()),
                            origTask.getIndexInTaskType(),
                            customerByIdMap.get(origTask.getCustomer().getId()),
                            origTask.getReadyTime(),
                            Priority.valueOf(origTask.getPriority().name()));
                    task.setStartTime(origTask.getStartTime());
                    // Employee is null if the data set is uninitialized.
                    Optional.ofNullable(origTask.getEmployee())
                            .ifPresent(employee -> task.setEmployee(employeeByIdMap.get(employee.getId())));
                    // Index will be set later.
                    return task;
                })
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        for (org.optaplanner.examples.taskassigning.domain.Employee origEmployee : origSolution.getEmployeeList()) {
            Employee employee = employeeByIdMap.get(origEmployee.getId());
            employee.setTasks(new ArrayList<>());
            org.optaplanner.examples.taskassigning.domain.Task nextTask = origEmployee.getNextTask();
            int index = 0;
            while (nextTask != null) {
                Task task = taskByIdMap.get(nextTask.getId());
                // Set index.
                task.setIndex(index++);
                // Add task to the employee's taskList.
                employee.getTasks().add(task);
                nextTask = nextTask.getNextTask();
            }
        }

        return new TaskAssigningSolution(
                origSolution.getId(),
                new ArrayList<>(skillByIdMap.values()),
                new ArrayList<>(taskTypeByIdMap.values()),
                new ArrayList<>(customerByIdMap.values()),
                new ArrayList<>(employeeByIdMap.values()),
                new ArrayList<>(taskByIdMap.values()));
    }

    @Override
    public void write(TaskAssigningSolution taskAssigningSolution, File outputSolutionFile) {
        currentIO.write(taskAssigningSolution, outputSolutionFile);
    }

    @Override
    public String getInputFileExtension() {
        return "xml";
    }

}
