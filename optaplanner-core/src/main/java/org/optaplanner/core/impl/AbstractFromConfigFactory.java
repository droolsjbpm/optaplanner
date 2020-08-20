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

package org.optaplanner.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.config.AbstractConfig;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

public abstract class AbstractFromConfigFactory<Config_ extends AbstractConfig<Config_>> {

    protected final Config_ config;

    public AbstractFromConfigFactory(Config_ config) {
        this.config = config;
    }

    protected EntityDescriptor deduceEntityDescriptor(SolutionDescriptor solutionDescriptor, Class<?> entityClass) {
        EntityDescriptor entityDescriptor;
        entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(Objects.requireNonNull(entityClass));
        if (entityDescriptor == null) {
            throw new IllegalArgumentException("The config (" + config
                    + ") has an entityClass (" + entityClass + ") that is not a known planning entity.\n"
                    + "Check your solver configuration. If that class (" + entityClass.getSimpleName()
                    + ") is not in the entityClassSet (" + solutionDescriptor.getEntityClassSet()
                    + "), check your " + PlanningSolution.class.getSimpleName()
                    + " implementation's annotated methods too.");
        }
        return entityDescriptor;
    }

    protected EntityDescriptor deduceEntityDescriptor(SolutionDescriptor solutionDescriptor) {
        Collection<EntityDescriptor> entityDescriptors = solutionDescriptor.getGenuineEntityDescriptors();
        if (entityDescriptors.size() != 1) {
            throw new IllegalArgumentException("The config (" + config
                    + ") has no entityClass configured and because there are multiple in the entityClassSet ("
                    + solutionDescriptor.getEntityClassSet()
                    + "), it cannot be deduced automatically.");
        }
        return entityDescriptors.iterator().next();
    }

    protected GenuineVariableDescriptor deduceVariableDescriptor(EntityDescriptor entityDescriptor, String variableName) {
        GenuineVariableDescriptor variableDescriptor;
        variableDescriptor = entityDescriptor.getGenuineVariableDescriptor(Objects.requireNonNull(variableName));
        if (variableDescriptor == null) {
            throw new IllegalArgumentException("The config (" + config
                    + ") has a variableName (" + variableName
                    + ") which is not a valid planning variable on entityClass ("
                    + entityDescriptor.getEntityClass() + ").\n"
                    + entityDescriptor.buildInvalidVariableNameExceptionMessage(variableName));
        }
        return variableDescriptor;
    }

    protected GenuineVariableDescriptor deduceVariableDescriptor(EntityDescriptor entityDescriptor) {
        Collection<GenuineVariableDescriptor> variableDescriptors = entityDescriptor.getGenuineVariableDescriptors();
        if (variableDescriptors.size() != 1) {
            throw new IllegalArgumentException("The config (" + config
                    + ") has no configured variableName for entityClass (" + entityDescriptor.getEntityClass()
                    + ") and because there are multiple variableNames ("
                    + entityDescriptor.getGenuineVariableNameSet()
                    + "), it cannot be deduced automatically.");
        }
        return variableDescriptors.iterator().next();
    }

    protected List<GenuineVariableDescriptor> deduceVariableDescriptorList(EntityDescriptor entityDescriptor,
            List<String> variableNameIncludeList) {
        Objects.requireNonNull(entityDescriptor);
        List<GenuineVariableDescriptor> variableDescriptorList = entityDescriptor.getGenuineVariableDescriptorList();
        if (variableNameIncludeList == null) {
            return variableDescriptorList;
        }
        List<GenuineVariableDescriptor> resolvedVariableDescriptorList =
                new ArrayList<>(variableDescriptorList.size());
        for (String variableNameInclude : variableNameIncludeList) {
            boolean found = false;
            for (GenuineVariableDescriptor variableDescriptor : variableDescriptorList) {
                if (variableDescriptor.getVariableName().equals(variableNameInclude)) {
                    resolvedVariableDescriptorList.add(variableDescriptor);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("The config (" + this
                        + ") has a variableNameInclude (" + variableNameInclude
                        + ") which does not exist in the entity (" + entityDescriptor.getEntityClass()
                        + ")'s variableDescriptorList (" + variableDescriptorList + ").");
            }
        }
        return resolvedVariableDescriptorList;
    }
}
