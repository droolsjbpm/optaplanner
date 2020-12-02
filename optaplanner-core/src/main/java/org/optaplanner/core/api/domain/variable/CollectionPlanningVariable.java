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

package org.optaplanner.core.api.domain.variable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;

import org.optaplanner.core.api.domain.entity.PlanningEntity;

/**
 * Specifies that a bean property (or a field) of a {@link Collection} type should be optimized by the optimization
 * algorithms. Unlike {@link PlanningVariable}, the {@link CollectionPlanningVariable} tells OptaPlanner to change
 * items inside the collection variable instead of changing which collection is assigned to the variable.
 * <p>
 * It is specified on a getter of a java bean property (or directly on a field) of a {@link PlanningEntity} class.
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface CollectionPlanningVariable {
    String[] valueRangeProviderRefs() default {};

    // TODO value strength
}
