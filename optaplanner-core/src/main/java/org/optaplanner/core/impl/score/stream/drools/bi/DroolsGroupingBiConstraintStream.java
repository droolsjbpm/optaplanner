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

package org.optaplanner.core.impl.score.stream.drools.bi;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.optaplanner.core.api.score.stream.bi.BiConstraintCollector;
import org.optaplanner.core.api.score.stream.uni.UniConstraintCollector;
import org.optaplanner.core.impl.score.stream.drools.DroolsConstraintFactory;
import org.optaplanner.core.impl.score.stream.drools.common.DroolsAbstractConstraintStream;
import org.optaplanner.core.impl.score.stream.drools.uni.DroolsAbstractUniConstraintStream;

public class DroolsGroupingBiConstraintStream<Solution_, NewA, NewB>
        extends DroolsAbstractBiConstraintStream<Solution_, NewA, NewB> {

    private final DroolsAbstractConstraintStream<Solution_> parent;
    private final DroolsBiCondition<NewA, NewB> condition;

    public <A> DroolsGroupingBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractUniConstraintStream<Solution_, A> parent, Function<A, NewA> groupKeyAMapping,
            Function<A, NewB> groupKeyBMapping) {
        super(constraintFactory);
        this.parent = parent;
        this.condition = parent.getCondition().andGroupBi(groupKeyAMapping, groupKeyBMapping);
    }

    public <A, __> DroolsGroupingBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractUniConstraintStream<Solution_, A> parent, Function<A, NewA> groupKeyMapping,
            UniConstraintCollector<A, __, NewB> collector) {
        super(constraintFactory);
        this.parent = parent;
        this.condition = parent.getCondition().andGroupWithCollect(groupKeyMapping, collector);
    }

    public <A, B, __> DroolsGroupingBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractBiConstraintStream<Solution_, A, B> parent, BiFunction<A, B, NewA> groupKeyMapping,
            BiConstraintCollector<A, B, __, NewB> collector) {
        super(constraintFactory);
        this.parent = parent;
        this.condition = parent.getCondition().andGroupWithCollect(groupKeyMapping, collector);
    }

    @Override
    public DroolsBiCondition<NewA, NewB> getCondition() {
        return condition;
    }

    @Override
    protected DroolsAbstractConstraintStream<Solution_> getParent() {
        return parent;
    }

    @Override
    public boolean isGroupByAllowed() {
        return false;
    }

    @Override
    public String toString() {
        return "BiGroup() with " + getChildStreams().size() + " children";
    }
}
