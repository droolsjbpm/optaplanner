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

package org.optaplanner.core.config.phase.loop;

import java.util.List;
import java.util.function.Consumer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import org.optaplanner.core.config.phase.NoChangePhaseConfig;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.phase.custom.CustomPhaseConfig;
import org.optaplanner.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "phaseConfigList",
})
public class LoopPhaseConfig extends PhaseConfig<LoopPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "loop";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    @XmlElements({
            @XmlElement(name = ConstructionHeuristicPhaseConfig.XML_ELEMENT_NAME,
                    type = ConstructionHeuristicPhaseConfig.class),
            @XmlElement(name = CustomPhaseConfig.XML_ELEMENT_NAME, type = CustomPhaseConfig.class),
            @XmlElement(name = ExhaustiveSearchPhaseConfig.XML_ELEMENT_NAME, type = ExhaustiveSearchPhaseConfig.class),
            @XmlElement(name = LocalSearchPhaseConfig.XML_ELEMENT_NAME, type = LocalSearchPhaseConfig.class),
            @XmlElement(name = NoChangePhaseConfig.XML_ELEMENT_NAME,
                    type = NoChangePhaseConfig.class),
            @XmlElement(name = PartitionedSearchPhaseConfig.XML_ELEMENT_NAME, type = PartitionedSearchPhaseConfig.class),
            @XmlElement(name = LoopPhaseConfig.XML_ELEMENT_NAME, type = LoopPhaseConfig.class),
    })
    private List<PhaseConfig> phaseConfigList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public List<PhaseConfig> getPhaseConfigList() {
        return phaseConfigList;
    }

    public void setPhaseConfigList(List<PhaseConfig> phaseConfigList) {
        this.phaseConfigList = phaseConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public LoopPhaseConfig withPhaseConfigList(List<PhaseConfig> phaseConfigList) {
        this.phaseConfigList = phaseConfigList;
        return this;
    }

    @Override
    public LoopPhaseConfig inherit(LoopPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        phaseConfigList = ConfigUtils.inheritMergeableListProperty(
                phaseConfigList, inheritedConfig.getPhaseConfigList());
        return this;
    }

    @Override
    public LoopPhaseConfig copyConfig() {
        return new LoopPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        if (getTerminationConfig() != null) {
            getTerminationConfig().visitReferencedClasses(classVisitor);
        }
        if (phaseConfigList != null) {
            phaseConfigList.forEach(c -> c.visitReferencedClasses(classVisitor));
        }
    }

}
