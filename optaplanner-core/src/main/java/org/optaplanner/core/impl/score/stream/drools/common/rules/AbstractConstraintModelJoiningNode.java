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

package org.optaplanner.core.impl.score.stream.drools.common.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public abstract class AbstractConstraintModelJoiningNode<LeftHandSide_ extends AbstractLeftHandSide, OtherFactType_, JoinerType_>
        extends AbstractConstraintModelChildNode<LeftHandSide_> implements Supplier<List<JoinerType_>> {

    private final Class<OtherFactType_> otherFactType;
    private final List<JoinerType_> joiner;

    AbstractConstraintModelJoiningNode(LeftHandSide_ leftHandSide, Class<OtherFactType_> otherFactType,
                                       ConstraintGraphNodeType type, JoinerType_... joiner) {
        super(leftHandSide);
        if (type != ConstraintGraphNodeType.IF_EXISTS && type != ConstraintGraphNodeType.IF_NOT_EXISTS &&
                type != ConstraintGraphNodeType.JOIN) {
            throw new IllegalStateException("Impossible state: Given node type (" + type +
                    ") is not one of the join types.");
        }
        this.otherFactType = requireNonNull(otherFactType);
        this.joiner = Collections.unmodifiableList(Arrays.asList(joiner));
    }

    public final Class<OtherFactType_> getOtherFactType() {
        return otherFactType;
    }

    @Override
    public final List<JoinerType_> get() {
        return joiner;
    }
}
