/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.config.heuristic.selector.entity.pillar;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.BooleanUtils;
import org.optaplanner.core.config.heuristic.policy.HeuristicConfigPolicy;
import org.optaplanner.core.config.heuristic.selector.SelectorConfig;
import org.optaplanner.core.config.heuristic.selector.common.SelectionCacheType;
import org.optaplanner.core.config.heuristic.selector.common.SelectionOrder;
import org.optaplanner.core.config.heuristic.selector.entity.EntitySelectorConfig;
import org.optaplanner.core.config.heuristic.selector.move.generic.PillarType;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.heuristic.selector.entity.EntitySelector;
import org.optaplanner.core.impl.heuristic.selector.entity.pillar.DefaultPillarSelector;
import org.optaplanner.core.impl.heuristic.selector.entity.pillar.PillarSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@XStreamAlias("pillarSelector")
public class PillarSelectorConfig extends SelectorConfig<PillarSelectorConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PillarSelectorConfig.class);

    @XStreamAlias("entitySelector")
    protected EntitySelectorConfig entitySelectorConfig = null;

    protected Boolean subPillarEnabled = null;
    protected Integer minimumSubPillarSize = null;
    protected Integer maximumSubPillarSize = null;
    protected Class<? extends Comparator> pillarOrderComparatorClass = null;

    public EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    /**
     * @see PillarType and its uses in pillar move selectors.
     */
    @Deprecated(/* forRemoval = true */)
    public Boolean getSubPillarEnabled() {
        return subPillarEnabled;
    }

    /**
     * @see PillarType and its uses in pillar move selectors.
     */
    @Deprecated(/* forRemoval = true */)
    public void setSubPillarEnabled(Boolean subPillarEnabled) {
        this.subPillarEnabled = subPillarEnabled;
    }

    public Integer getMinimumSubPillarSize() {
        return minimumSubPillarSize;
    }

    public void setMinimumSubPillarSize(Integer minimumSubPillarSize) {
        this.minimumSubPillarSize = minimumSubPillarSize;
    }

    public Integer getMaximumSubPillarSize() {
        return maximumSubPillarSize;
    }

    public void setMaximumSubPillarSize(Integer maximumSubPillarSize) {
        this.maximumSubPillarSize = maximumSubPillarSize;
    }

    public Class<? extends Comparator> getPillarOrderComparatorClass() {
        return pillarOrderComparatorClass;
    }

    public void setPillarOrderComparatorClass(final Class<? extends Comparator> pillarOrderComparatorClass) {
        this.pillarOrderComparatorClass = pillarOrderComparatorClass;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    /**
     * @param configPolicy never null
     * @param pillarType if null, defaults to {@link PillarType#FULL_AND_SUB} for backwards compatibility reasons.
     * @param minimumCacheType never null, If caching is used (different from {@link SelectionCacheType#JUST_IN_TIME}),
     * then it should be at least this {@link SelectionCacheType} because an ancestor already uses such caching
     * and less would be pointless.
     * @param inheritedSelectionOrder never null
     * @param variableNameIncludeList sometimes null
     * @return never null
     */
    public PillarSelector buildPillarSelector(HeuristicConfigPolicy configPolicy, PillarType pillarType,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder,
            List<String> variableNameIncludeList) {
        if (subPillarEnabled != null) {
            if (pillarType == null) {
                LOGGER.warn("Property subPillarEnabled ({}) on pillarSelectorConfig ({}) is deprecated for removal.\n" +
                        "Use property pillarType on the parent MoveSelectorConfig.", subPillarEnabled, this);
            } else {
                throw new IllegalArgumentException("Property subPillarEnabled (" + subPillarEnabled +
                        ") on pillarSelectorConfig (" + this + ") must not be present when pillarType (" + pillarType +
                        ") is set on the parent MoveSelectorConfig.");
            }
        }
        if (pillarType != PillarType.SEQUENTIAL && pillarOrderComparatorClass != null) {
            throw new IllegalArgumentException("Pillar type (" + pillarType + ") on pillarSelectorConfig (" + this +
                    ") is not " + PillarType.SEQUENTIAL + ", yet pillarOrderComparatorClass (" +
                    pillarOrderComparatorClass + ") is provided.");
        }
        if (minimumCacheType.compareTo(SelectionCacheType.STEP) > 0) {
            throw new IllegalArgumentException("The pillarSelectorConfig (" + this
                    + ")'s minimumCacheType (" + minimumCacheType
                    + ") must not be higher than " + SelectionCacheType.STEP
                    + " because the pillars change every step.");
        }
        boolean subPillarActuallyEnabled = BooleanUtils.isTrue(subPillarEnabled) || pillarType != PillarType.FULL_ONLY;
        // EntitySelector uses SelectionOrder.ORIGINAL because a DefaultPillarSelector STEP caches the values
        EntitySelectorConfig entitySelectorConfig_ = entitySelectorConfig == null ? new EntitySelectorConfig()
                : entitySelectorConfig;
        EntitySelector entitySelector = entitySelectorConfig_.buildEntitySelector(configPolicy,
                minimumCacheType, SelectionOrder.ORIGINAL);
        Collection<GenuineVariableDescriptor> variableDescriptors = deduceVariableDescriptorList(
                entitySelector.getEntityDescriptor(), variableNameIncludeList);
        if (!subPillarActuallyEnabled
                && (minimumSubPillarSize != null || maximumSubPillarSize != null)) {
            throw new IllegalArgumentException("The pillarSelectorConfig (" + this
                    + ") must not disable subpillars while providing minimumSubPillarSize (" + minimumSubPillarSize
                    + ") or maximumSubPillarSize (" + maximumSubPillarSize + ").");
        }

        SubPillarConfigPolicy subPillarPolicy = subPillarActuallyEnabled ?
                configureSubPillars(pillarType, entitySelector, minimumSubPillarSize, maximumSubPillarSize) :
                SubPillarConfigPolicy.withoutSubpillars();
        return new DefaultPillarSelector(entitySelector, variableDescriptors,
                inheritedSelectionOrder.toRandomSelectionBoolean(), subPillarPolicy);
    }

    private SubPillarConfigPolicy configureSubPillars(PillarType pillarType, EntitySelector entitySelector,
            Integer minimumSubPillarSize, Integer maximumSubPillarSize) {
        int actualMinimumSubPillarSize = defaultIfNull(minimumSubPillarSize, 1);
        int actualMaximumSubPillarSize = defaultIfNull(maximumSubPillarSize, Integer.MAX_VALUE);
        if (pillarType == null) { // for backwards compatibility reasons
            return SubPillarConfigPolicy.withSubpillars(actualMinimumSubPillarSize, actualMaximumSubPillarSize);
        }
        switch (pillarType) {
            case FULL_AND_SUB:
                return SubPillarConfigPolicy.withSubpillars(actualMinimumSubPillarSize, actualMaximumSubPillarSize);
            case SEQUENTIAL:
                if (pillarOrderComparatorClass == null) {
                    Class<?> entityClass = entitySelector.getEntityDescriptor().getEntityClass();
                    boolean isComparable = entityClass.isAssignableFrom(Comparable.class);
                    if (!isComparable) {
                        throw new IllegalArgumentException("Pillar type (" + pillarType + ") on pillarSelectorConfig (" +
                                this + ") does not provide pillarOrderComparatorClass while the entity (" +
                                entityClass.getCanonicalName() + ") does not implement Comparable.");
                    }
                    Comparator<Comparable> comparator = Comparable::compareTo;
                    return SubPillarConfigPolicy.sequential(actualMinimumSubPillarSize, actualMaximumSubPillarSize,
                            comparator);
                } else {
                    Comparator<Object> comparator = ConfigUtils.newInstance(this, "pillarOrderComparatorClass", pillarOrderComparatorClass);
                    return SubPillarConfigPolicy.sequential(actualMinimumSubPillarSize, actualMaximumSubPillarSize,
                            comparator);
                }
            default:
                throw new IllegalStateException("Subpillars can not be enabled and disabled at the same time.");
        }
    }

    @Override
    public void inherit(PillarSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        subPillarEnabled = ConfigUtils.inheritOverwritableProperty(subPillarEnabled,
                inheritedConfig.getSubPillarEnabled());
        minimumSubPillarSize = ConfigUtils.inheritOverwritableProperty(minimumSubPillarSize,
                inheritedConfig.getMinimumSubPillarSize());
        maximumSubPillarSize = ConfigUtils.inheritOverwritableProperty(maximumSubPillarSize,
                inheritedConfig.getMaximumSubPillarSize());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelectorConfig + ")";
    }
}
