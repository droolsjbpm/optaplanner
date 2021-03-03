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

package org.optaplanner.core.impl.score.stream.bavet.uni;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.bi.BiConstraintStream;
import org.optaplanner.core.api.score.stream.bi.BiJoiner;
import org.optaplanner.core.api.score.stream.quad.QuadConstraintStream;
import org.optaplanner.core.api.score.stream.tri.TriConstraintStream;
import org.optaplanner.core.api.score.stream.uni.UniConstraintCollector;
import org.optaplanner.core.api.score.stream.uni.UniConstraintStream;
import org.optaplanner.core.impl.score.stream.bavet.BavetConstraint;
import org.optaplanner.core.impl.score.stream.bavet.BavetConstraintFactory;
import org.optaplanner.core.impl.score.stream.bavet.bi.BavetGroupBiConstraintStream;
import org.optaplanner.core.impl.score.stream.bavet.bi.BavetJoinBiConstraintStream;
import org.optaplanner.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import org.optaplanner.core.impl.score.stream.bavet.common.BavetNodeBuildPolicy;
import org.optaplanner.core.impl.score.stream.bavet.common.index.BavetIndexFactory;
import org.optaplanner.core.impl.score.stream.bi.AbstractBiJoiner;
import org.optaplanner.core.impl.score.stream.bi.FilteringBiJoiner;
import org.optaplanner.core.impl.score.stream.common.JoinerType;
import org.optaplanner.core.impl.score.stream.common.ScoreImpactType;
import org.optaplanner.core.impl.score.stream.uni.InnerUniConstraintStream;

public abstract class BavetAbstractUniConstraintStream<Solution_, A> extends BavetAbstractConstraintStream<Solution_>
        implements InnerUniConstraintStream<A> {

    protected final List<BavetAbstractUniConstraintStream<Solution_, A>> childStreamList = new ArrayList<>(2);

    public BavetAbstractUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory) {
        super(constraintFactory);
    }

    // ************************************************************************
    // Stream builder methods
    // ************************************************************************

    public void addChildStream(BavetAbstractUniConstraintStream<Solution_, A> childStream) {
        childStreamList.add(childStream);
    }

    @Override
    public int getCardinality() {
        return 1;
    }

    // ************************************************************************
    // Filter
    // ************************************************************************

    @Override
    public BavetAbstractUniConstraintStream<Solution_, A> filter(Predicate<A> predicate) {
        BavetFilterUniConstraintStream<Solution_, A> stream = new BavetFilterUniConstraintStream<>(constraintFactory, this,
                predicate);
        childStreamList.add(stream);
        return stream;
    }

    // ************************************************************************
    // Join
    // ************************************************************************

    @Override
    public <B> BiConstraintStream<A, B> join(UniConstraintStream<B> otherStream, BiJoiner<A, B> joiner) {
        if (!(otherStream instanceof BavetAbstractUniConstraintStream)) {
            throw new IllegalStateException("The streams (" + this + ", " + otherStream
                    + ") are not build from the same " + ConstraintFactory.class.getSimpleName() + ".");
        }
        BavetAbstractUniConstraintStream<Solution_, B> other = (BavetAbstractUniConstraintStream<Solution_, B>) otherStream;
        if (constraintFactory != other.getConstraintFactory()) {
            throw new IllegalStateException("The streams (" + this + ", " + other
                    + ") are build from different constraintFactories (" + constraintFactory + ", "
                    + other.getConstraintFactory()
                    + ").");
        }
        if (!(joiner instanceof AbstractBiJoiner)) {
            throw new IllegalArgumentException("The joiner class (" + joiner.getClass() + ") is not supported.");
        } else if (joiner instanceof FilteringBiJoiner) {
            return join(otherStream)
                    .filter(((FilteringBiJoiner<A, B>) joiner).getFilter());
        }
        AbstractBiJoiner<A, B> castedJoiner = (AbstractBiJoiner<A, B>) joiner;
        for (JoinerType type : castedJoiner.getJoinerTypes()) {
            switch (type) {
                case EQUAL:
                case LESS_THAN:
                case LESS_THAN_OR_EQUAL:
                case GREATER_THAN:
                case GREATER_THAN_OR_EQUAL:
                    continue;
                default:
                    throw new UnsupportedOperationException("Unsupported joiner type (" + type + ").");
            }
        }
        BavetIndexFactory indexFactory = new BavetIndexFactory(castedJoiner);
        BavetJoinBridgeUniConstraintStream<Solution_, A> leftBridge = new BavetJoinBridgeUniConstraintStream<>(
                constraintFactory, this, true, castedJoiner.getLeftCombinedMapping(), indexFactory);
        childStreamList.add(leftBridge);
        BavetJoinBridgeUniConstraintStream<Solution_, B> rightBridge = new BavetJoinBridgeUniConstraintStream<>(
                constraintFactory, other, false, castedJoiner.getRightCombinedMapping(), indexFactory);
        other.childStreamList.add(rightBridge);
        BavetJoinBiConstraintStream<Solution_, A, B> joinStream = new BavetJoinBiConstraintStream<>(constraintFactory,
                leftBridge, rightBridge);
        leftBridge.setJoinStream(joinStream);
        rightBridge.setJoinStream(joinStream);
        return joinStream;
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    @SafeVarargs
    @Override
    public final <B> UniConstraintStream<A> ifExists(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        throw new UnsupportedOperationException();
    }

    @SafeVarargs
    @Override
    public final <B> UniConstraintStream<A> ifNotExists(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        throw new UnsupportedOperationException();
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
            UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(Function<A, GroupKey_> groupKeyMapping) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(Function<A, GroupKey_> groupKeyMapping,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(Function<A, GroupKey_> groupKeyMapping, UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            Function<A, GroupKey_> groupKeyMapping,
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        BavetGroupBridgeUniConstraintStream<Solution_, A, GroupKey_, ResultContainer_, Result_> bridge =
                new BavetGroupBridgeUniConstraintStream<>(constraintFactory, this, groupKeyMapping, collector);
        childStreamList.add(bridge);
        BavetGroupBiConstraintStream<Solution_, GroupKey_, ResultContainer_, Result_> groupStream =
                new BavetGroupBiConstraintStream<>(constraintFactory, bridge, collector.finisher());
        bridge.setGroupStream(groupStream);
        return groupStream;
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    Function<A, GroupKeyA_> groupKeyAMapping,
                    Function<A, GroupKeyB_> groupKeyBMapping, UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        throw new UnsupportedOperationException(); // TODO
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @Override
    public final Constraint impactScore(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraint(constraintPackage, constraintName, constraintWeight,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScore(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToIntFunction<A> matchWeigher, ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraint(constraintPackage, constraintName, constraintWeight,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint, matchWeigher);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScoreLong(String constraintPackage, String constraintName, Score<?> constraintWeight,
            ToLongFunction<A> matchWeigher, ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraint(constraintPackage, constraintName, constraintWeight,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint, matchWeigher);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScoreBigDecimal(String constraintPackage, String constraintName,
            Score<?> constraintWeight, Function<A, BigDecimal> matchWeigher, ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraint(constraintPackage, constraintName, constraintWeight,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint, matchWeigher);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScoreConfigurable(String constraintPackage, String constraintName,
            ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraintConfigurable(constraintPackage, constraintName,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScoreConfigurable(String constraintPackage, String constraintName,
            ToIntFunction<A> matchWeigher, ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraintConfigurable(constraintPackage, constraintName,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint, matchWeigher);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScoreConfigurableLong(String constraintPackage, String constraintName,
            ToLongFunction<A> matchWeigher, ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraintConfigurable(constraintPackage, constraintName,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint, matchWeigher);
        childStreamList.add(stream);
        return constraint;
    }

    @Override
    public final Constraint impactScoreConfigurableBigDecimal(String constraintPackage, String constraintName,
            Function<A, BigDecimal> matchWeigher, ScoreImpactType impactType) {
        BavetConstraint<Solution_> constraint = buildConstraintConfigurable(constraintPackage, constraintName,
                impactType);
        BavetScoringUniConstraintStream<Solution_, A> stream = new BavetScoringUniConstraintStream<>(constraintFactory,
                this, constraint, matchWeigher);
        childStreamList.add(stream);
        return constraint;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    public BavetAbstractUniNode<A> createNodeChain(BavetNodeBuildPolicy<Solution_> buildPolicy,
            Score<?> constraintWeight, BavetAbstractUniNode<A> parentNode) {
        BavetAbstractUniNode<A> node = createNode(buildPolicy, constraintWeight, parentNode);
        node = processNode(buildPolicy, parentNode, node);
        createChildNodeChains(buildPolicy, constraintWeight, node);
        return node;
    }

    protected BavetAbstractUniNode<A> processNode(BavetNodeBuildPolicy<Solution_> buildPolicy,
            BavetAbstractUniNode<A> parentNode, BavetAbstractUniNode<A> node) {
        BavetAbstractUniNode<A> sharedNode = buildPolicy.retrieveSharedNode(node);
        if (sharedNode != node) { // Share node
            return sharedNode;
        }
        if (parentNode != null) { // TODO remove null check and don't go through this for from and joins
            parentNode.addChildNode(node);
        }
        return node;
    }

    protected void createChildNodeChains(BavetNodeBuildPolicy<Solution_> buildPolicy, Score<?> constraintWeight,
            BavetAbstractUniNode<A> node) {
        if (childStreamList.isEmpty()) {
            throw new IllegalStateException("The stream (" + this + ") leads to nowhere.\n"
                    + "Maybe don't create it.");
        }
        for (BavetAbstractUniConstraintStream<Solution_, A> childStream : childStreamList) {
            childStream.createNodeChain(buildPolicy, constraintWeight, node);
        }
    }

    protected abstract BavetAbstractUniNode<A> createNode(BavetNodeBuildPolicy<Solution_> buildPolicy,
            Score<?> constraintWeight, BavetAbstractUniNode<A> parentNode);

}
