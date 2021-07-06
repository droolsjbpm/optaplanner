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

package org.optaplanner.core.config.solver.metric;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.optaplanner.core.config.AbstractConfig;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;

@XmlType(propOrder = {
        "solverMetricList",
        "tagNameToValueMap"
})
public class MetricConfig extends AbstractConfig<MetricConfig> {
    @XmlElement(name = "metrics")
    protected List<SolverMetric> solverMetricList = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> tagNameToValueMap = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************
    public List<SolverMetric> getSolverMetricList() {
        return solverMetricList;
    }

    public void setSolverMetricList(List<SolverMetric> solverMetricList) {
        this.solverMetricList = solverMetricList;
    }

    public Map<String, String> getTagNameToValueMap() {
        return tagNameToValueMap;
    }

    public void setTagNameToValueMap(Map<String, String> tagNameToValueMap) {
        this.tagNameToValueMap = tagNameToValueMap;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public MetricConfig withSolverMetricList(List<SolverMetric> solverMetricList) {
        this.solverMetricList = solverMetricList;
        return this;
    }

    public MetricConfig withTagNameToValueMap(Map<String, String> tagNameToValueMap) {
        this.tagNameToValueMap = tagNameToValueMap;
        return this;
    }

    @Override
    public MetricConfig inherit(MetricConfig inheritedConfig) {
        solverMetricList = ConfigUtils.inheritMergeableListProperty(solverMetricList, inheritedConfig.solverMetricList);
        tagNameToValueMap = ConfigUtils.inheritMergeableMapProperty(tagNameToValueMap, inheritedConfig.tagNameToValueMap);
        return this;
    }

    @Override
    public MetricConfig copyConfig() {
        return new MetricConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        // No referenced classes currently
        // If we add custom metrics here, then this should
        // register the custom metrics
    }
}
