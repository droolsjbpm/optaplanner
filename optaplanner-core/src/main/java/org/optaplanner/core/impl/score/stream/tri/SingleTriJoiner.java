/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.score.stream.tri;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.core.api.score.stream.common.JoinerType;
import org.optaplanner.core.impl.score.stream.bi.AbstractBiJoiner;

public final class SingleTriJoiner<A, B, C> extends AbstractTriJoiner<A, B, C> {

    private final BiFunction<A, B, ?> leftMapping;
    private final JoinerType joinerType;
    private final Function<C, ?> rightMapping;

    public <Property_> SingleTriJoiner(BiFunction<A, B, Property_> leftMapping, JoinerType joinerType, Function<C, Property_> rightMapping) {
        this.leftMapping = leftMapping;
        this.joinerType = joinerType;
        this.rightMapping = rightMapping;
    }

    public BiFunction<A, B, ?> getLeftMapping() {
        return leftMapping;
    }

    public JoinerType getJoinerType() {
        return joinerType;
    }

    public Function<C, ?> getRightMapping() {
        return rightMapping;
    }

    // ************************************************************************
    // Builders
    // ************************************************************************

    @Override
    public BiFunction<A, B, Object[]> getLeftCombinedMapping() {
        return (A a, B b) -> new Object[]{leftMapping.apply(a, b)};
    }

    @Override
    public JoinerType[] getJoinerTypes() {
        return new JoinerType[]{joinerType};
    }

    @Override
    public Function<C, Object[]> getRightCombinedMapping() {
        return (C c) -> new Object[]{rightMapping.apply(c)};
    }

}
