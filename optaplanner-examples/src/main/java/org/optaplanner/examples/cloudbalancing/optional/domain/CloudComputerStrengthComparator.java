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

package org.optaplanner.examples.cloudbalancing.optional.domain;

import java.io.Serializable;
import java.util.Comparator;

import org.optaplanner.core.api.score.constraint.ConstraintJustification;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;

public class CloudComputerStrengthComparator implements Comparator<CloudComputer>, Serializable {

    private static final Comparator<CloudComputer> DESCENDING_COST_COMPARATOR =
            Comparator.comparing(CloudComputer::getCost).reversed();

    private static final Comparator<CloudComputer> COMPARATOR =
            Comparator.comparingInt(CloudComputer::getMultiplicand)
            .thenComparing(DESCENDING_COST_COMPARATOR) // Descending (but this is debatable)
            .thenComparing(ConstraintJustification.COMPARATOR);

    @Override
    public int compare(CloudComputer a, CloudComputer b) {
        return COMPARATOR.compare(a, b);
    }

}
