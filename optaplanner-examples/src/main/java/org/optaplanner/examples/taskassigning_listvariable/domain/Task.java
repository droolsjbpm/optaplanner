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

package org.optaplanner.examples.taskassigning_listvariable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;
import org.optaplanner.examples.common.domain.AbstractPersistable;
import org.optaplanner.examples.common.swingui.components.Labeled;
import org.optaplanner.examples.taskassigning_listvariable.domain.solver.CustomCollectionInverseRelationShadowVariableListener;
import org.optaplanner.examples.taskassigning_listvariable.domain.solver.StartTimeUpdatingVariableListener;
import org.optaplanner.examples.taskassigning_listvariable.domain.solver.TaskDifficultyComparator;

import com.thoughtworks.xstream.annotations.XStreamAlias;

// TODO convert entity difficulty to value strength
@PlanningEntity(difficultyComparatorClass = TaskDifficultyComparator.class)
@XStreamAlias("TaTask")
public class Task extends AbstractPersistable implements Labeled {

    private TaskType taskType;
    private int indexInTaskType;
    private Customer customer;
    private int readyTime;
    private Priority priority;
    // TODO keep this and make it possible to pin values?
    @PlanningPin
    private boolean pinned;

    // Shadow variables
    // This maybe duplicates @InverseRelationshipShadowVariable. The field type may be either `Employee` or `Set<Employee>`
    // depending on whether the genuine `@PlanningVariable List<Task> tasks` is disjoint or not (for task assignment it
    // is disjoint, for other domains it may not be).
    // FIXME temporary custom shadow variable listener, will be replaced by a generic one
    @CustomShadowVariable(
            variableListenerClass = CustomCollectionInverseRelationShadowVariableListener.class,
            sources = @PlanningVariableReference(entityClass = Employee.class, variableName = "tasks"))
    // TODO the entity (Employee) or just the collection variable (List<Task>)?
    private Employee employee;
    // FIXME temporary custom shadow variable listener, will be replaced by a generic one
    @CustomShadowVariable(variableListenerRef = @PlanningVariableReference(variableName = "employee"))
    private Integer index;
    @CustomShadowVariable(variableListenerClass = StartTimeUpdatingVariableListener.class,
            // Arguable, to adhere to API specs (although this works), nextTask and employee should also be a source,
            // because this shadow must be triggered after nextTask and employee (but there is no need to be triggered by those)
            sources = { @PlanningVariableReference(entityClass = Employee.class, variableName = "tasks") })
    private Integer startTime; // In minutes

    public Task() {
    }

    public Task(long id, TaskType taskType, int indexInTaskType, Customer customer, int readyTime, Priority priority) {
        super(id);
        this.taskType = taskType;
        this.indexInTaskType = indexInTaskType;
        this.customer = customer;
        this.readyTime = readyTime;
        this.priority = priority;
        pinned = false;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public int getIndexInTaskType() {
        return indexInTaskType;
    }

    public void setIndexInTaskType(int indexInTaskType) {
        this.indexInTaskType = indexInTaskType;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public int getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(int readyTime) {
        this.readyTime = readyTime;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public int getMissingSkillCount() {
        if (employee == null) {
            return 0;
        }
        int count = 0;
        for (Skill skill : taskType.getRequiredSkillList()) {
            if (!employee.getSkillSet().contains(skill)) {
                count++;
            }
        }
        return count;
    }

    /**
     * In minutes
     *
     * @return at least 1 minute
     */
    public int getDuration() {
        Affinity affinity = getAffinity();
        return taskType.getBaseDuration() * affinity.getDurationMultiplier();
    }

    public Affinity getAffinity() {
        return (employee == null) ? Affinity.NONE : employee.getAffinity(customer);
    }

    public Integer getEndTime() {
        if (startTime == null) {
            return null;
        }
        return startTime + getDuration();
    }

    public String getCode() {
        return taskType + "-" + indexInTaskType;
    }

    public String getTitle() {
        return taskType.getTitle();
    }

    @Override
    public String getLabel() {
        return getCode() + ": " + taskType.getTitle();
    }

    public String getToolText() {
        StringBuilder toolText = new StringBuilder();
        toolText.append("<html><center><b>").append(getLabel()).append("</b><br/>")
                .append(priority.getLabel()).append("<br/><br/>");
        toolText.append("Required skills:<br/>");
        for (Skill skill : taskType.getRequiredSkillList()) {
            toolText.append(skill.getLabel()).append("<br/>");
        }
        toolText.append("<br/>Customer:<br/>").append(customer.getName()).append("<br/>(")
                .append(getAffinity().getLabel()).append(")<br/>");
        toolText.append("</center></html>");
        return toolText.toString();
    }

    @Override
    public String toString() {
        return getCode();
    }

}
