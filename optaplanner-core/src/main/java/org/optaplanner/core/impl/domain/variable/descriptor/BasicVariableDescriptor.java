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

package org.optaplanner.core.impl.domain.variable.descriptor;

import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableGraphType;
import org.optaplanner.core.impl.domain.common.accessor.MemberAccessor;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.policy.DescriptorPolicy;

// TODO naming
public class BasicVariableDescriptor<Solution_> extends GenuineVariableDescriptor<Solution_> {

    private boolean chained;
    private boolean nullable;

    public BasicVariableDescriptor(EntityDescriptor<Solution_> entityDescriptor, MemberAccessor variableMemberAccessor) {
        super(entityDescriptor, variableMemberAccessor);
    }

    @Override
    protected void processPropertyAnnotations(DescriptorPolicy descriptorPolicy) {
        PlanningVariable planningVariableAnnotation = variableMemberAccessor.getAnnotation(PlanningVariable.class);
        processNullable(descriptorPolicy, planningVariableAnnotation);
        processChained(descriptorPolicy, planningVariableAnnotation);
        processValueRangeRefs(descriptorPolicy, planningVariableAnnotation::valueRangeProviderRefs);
        processStrength(descriptorPolicy, planningVariableAnnotation);
    }

    @Override
    public boolean isListVariable() {
        return false;
    }

    @Override
    public boolean isChained() {
        return chained;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public boolean acceptsValueType(Class<?> valueType) {
        return getVariablePropertyType().isAssignableFrom(valueType);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    private void processNullable(DescriptorPolicy descriptorPolicy, PlanningVariable planningVariableAnnotation) {
        nullable = planningVariableAnnotation.nullable();
        if (nullable && variableMemberAccessor.getType().isPrimitive()) {
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has a PlanningVariable annotated property (" + variableMemberAccessor.getName()
                    + ") with nullable (" + nullable + "), which is not compatible with the primitive propertyType ("
                    + variableMemberAccessor.getType() + ").");
        }
    }

    private void processChained(DescriptorPolicy descriptorPolicy, PlanningVariable planningVariableAnnotation) {
        chained = planningVariableAnnotation.graphType() == PlanningVariableGraphType.CHAINED;
        if (chained && !variableMemberAccessor.getType().isAssignableFrom(
                entityDescriptor.getEntityClass())) {
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has a PlanningVariable annotated property (" + variableMemberAccessor.getName()
                    + ") with chained (" + chained + ") and propertyType (" + variableMemberAccessor.getType()
                    + ") which is not a superclass/interface of or the same as the entityClass ("
                    + entityDescriptor.getEntityClass() + ").\n"
                    + "If an entity's chained planning variable cannot point to another entity of the same class,"
                    + " then it is impossible to make chain longer than 1 entity and therefore chaining is useless.");
        }
        if (chained && nullable) {
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has a PlanningVariable annotated property (" + variableMemberAccessor.getName()
                    + ") with chained (" + chained + "), which is not compatible with nullable (" + nullable + ").");
        }
    }

    @Override
    public boolean isInitialized(Object entity) {
        if (isNullable()) {
            return true;
        }
        Object variable = getValue(entity);
        return variable != null;
    }
}
