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

package org.optaplanner.core.impl.score.stream.drools.bi;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import org.drools.model.Global;
import org.drools.model.RuleItemBuilder;
import org.optaplanner.core.api.score.holder.AbstractScoreHolder;
import org.optaplanner.core.impl.score.stream.drools.DroolsConstraintFactory;

public final class DroolsScoringBiConstraintStream<Solution_, A, B> extends DroolsAbstractBiConstraintStream<Solution_, A, B> {

    private final boolean noMatchWeigher;
    private final ToIntBiFunction<A, B> intMatchWeigher;
    private final ToLongBiFunction<A, B> longMatchWeigher;
    private final BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher;

    public DroolsScoringBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractBiConstraintStream<Solution_, A, B> parent) {
        this(constraintFactory, parent, true, null, null, null);
    }

    public DroolsScoringBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractBiConstraintStream<Solution_, A, B> parent, ToIntBiFunction<A, B> intMatchWeigher) {
        this(constraintFactory, parent, false, intMatchWeigher, null, null);
        if (intMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public DroolsScoringBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractBiConstraintStream<Solution_, A, B> parent, ToLongBiFunction<A, B> longMatchWeigher) {
        this(constraintFactory, parent, false, null, longMatchWeigher, null);
        if (longMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    public DroolsScoringBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractBiConstraintStream<Solution_, A, B> parent,
            BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher) {
        this(constraintFactory, parent, false, null, null, bigDecimalMatchWeigher);
        if (bigDecimalMatchWeigher == null) {
            throw new IllegalArgumentException("The matchWeigher (null) cannot be null.");
        }
    }

    private DroolsScoringBiConstraintStream(DroolsConstraintFactory<Solution_> constraintFactory,
            DroolsAbstractBiConstraintStream<Solution_, A, B> parent, boolean noMatchWeigher,
            ToIntBiFunction<A, B> intMatchWeigher, ToLongBiFunction<A, B> longMatchWeigher,
            BiFunction<A, B, BigDecimal> bigDecimalMatchWeigher) {
        super(constraintFactory, parent);
        this.noMatchWeigher = noMatchWeigher;
        this.intMatchWeigher = intMatchWeigher;
        this.longMatchWeigher = longMatchWeigher;
        this.bigDecimalMatchWeigher = bigDecimalMatchWeigher;
    }

    // ************************************************************************
    // Pattern creation
    // ************************************************************************

    @Override
    public void createRuleItemBuilders(List<RuleItemBuilder<?>> ruleItemBuilderList,
            Global<? extends AbstractScoreHolder> scoreHolderGlobal) {
        if (intMatchWeigher != null) {
            ruleItemBuilderList.addAll(getAnchor().terminateWithScoring(scoreHolderGlobal, intMatchWeigher));
        } else if (longMatchWeigher != null) {
            ruleItemBuilderList.addAll(getAnchor().terminateWithScoring(scoreHolderGlobal, longMatchWeigher));
        } else if (bigDecimalMatchWeigher != null) {
            ruleItemBuilderList.addAll(getAnchor().terminateWithScoring(scoreHolderGlobal, bigDecimalMatchWeigher));
        } else if (noMatchWeigher) {
            ruleItemBuilderList.addAll(getAnchor().terminateWithScoring(scoreHolderGlobal));
        } else {
            throw new IllegalStateException("Impossible state: noMatchWeigher (" + noMatchWeigher + ").");
        }
    }

    @Override
    public String toString() {
        return "BiScoring()";
    }

}
