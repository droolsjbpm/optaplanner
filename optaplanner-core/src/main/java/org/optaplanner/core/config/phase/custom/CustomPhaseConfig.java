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

package org.optaplanner.core.config.phase.custom;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;

@XmlType(propOrder = {
        "customPhaseCommandClassList",
        "customProperties",
})
public class CustomPhaseConfig<Solution_> extends PhaseConfig<Solution_, CustomPhaseConfig<Solution_>> {

    public static final String XML_ELEMENT_NAME = "customPhase";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    @XmlElement(name = "customPhaseCommandClass")
    protected List<Class<? extends CustomPhaseCommand<Solution_>>> customPhaseCommandClassList = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> customProperties = null;

    @XmlTransient
    protected List<CustomPhaseCommand<Solution_>> customPhaseCommandList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public List<Class<? extends CustomPhaseCommand<Solution_>>> getCustomPhaseCommandClassList() {
        return customPhaseCommandClassList;
    }

    public void
            setCustomPhaseCommandClassList(List<Class<? extends CustomPhaseCommand<Solution_>>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
    }

    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    public List<CustomPhaseCommand<Solution_>> getCustomPhaseCommandList() {
        return customPhaseCommandList;
    }

    public void setCustomPhaseCommandList(List<CustomPhaseCommand<Solution_>> customPhaseCommandList) {
        this.customPhaseCommandList = customPhaseCommandList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public CustomPhaseConfig<Solution_> withCustomPhaseCommandClassList(
            List<Class<? extends CustomPhaseCommand<Solution_>>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
        return this;
    }

    public void withCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    public CustomPhaseConfig<Solution_> withCustomPhaseCommandList(List<CustomPhaseCommand<Solution_>> customPhaseCommandList) {
        this.customPhaseCommandList = customPhaseCommandList;
        return this;
    }

    public CustomPhaseConfig<Solution_> withCustomPhaseCommands(CustomPhaseCommand<Solution_>... customPhaseCommands) {
        this.customPhaseCommandList = Arrays.asList(customPhaseCommands);
        return this;
    }

    @Override
    public CustomPhaseConfig<Solution_> inherit(CustomPhaseConfig<Solution_> inheritedConfig) {
        super.inherit(inheritedConfig);
        customPhaseCommandClassList = ConfigUtils.inheritMergeableListProperty(
                customPhaseCommandClassList, inheritedConfig.getCustomPhaseCommandClassList());
        customPhaseCommandList = ConfigUtils.inheritMergeableListProperty(
                customPhaseCommandList, inheritedConfig.getCustomPhaseCommandList());
        customProperties = ConfigUtils.inheritMergeableMapProperty(
                customProperties, inheritedConfig.getCustomProperties());
        return this;
    }

    @Override
    public CustomPhaseConfig<Solution_> copyConfig() {
        return new CustomPhaseConfig<Solution_>().inherit(this);
    }

}
