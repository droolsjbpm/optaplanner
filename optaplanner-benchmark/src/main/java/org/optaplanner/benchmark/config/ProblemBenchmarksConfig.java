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

package org.optaplanner.benchmark.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.BooleanUtils;
import org.optaplanner.benchmark.api.PlannerBenchmarkFactory;
import org.optaplanner.benchmark.config.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.config.statistic.SingleStatisticType;
import org.optaplanner.benchmark.impl.loader.FileProblemProvider;
import org.optaplanner.benchmark.impl.loader.InstanceProblemProvider;
import org.optaplanner.benchmark.impl.loader.ProblemProvider;
import org.optaplanner.benchmark.impl.result.PlannerBenchmarkResult;
import org.optaplanner.benchmark.impl.result.ProblemBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SingleBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SolverBenchmarkResult;
import org.optaplanner.benchmark.impl.result.SubSingleBenchmarkResult;
import org.optaplanner.benchmark.impl.statistic.ProblemStatistic;
import org.optaplanner.core.config.AbstractConfig;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.solver.DefaultSolverFactory;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

@XmlType(propOrder = {
        "solutionFileIOClass",
        "writeOutputSolutionEnabled",
        "inputSolutionFileList",
        "problemStatisticEnabled",
        "problemStatisticTypeList",
        "singleStatisticTypeList"
})
public class ProblemBenchmarksConfig<Solution_> extends AbstractConfig<Solution_, ProblemBenchmarksConfig<Solution_>> {

    private Class<SolutionFileIO<Solution_>> solutionFileIOClass = null;

    private Boolean writeOutputSolutionEnabled = null;

    @XmlElement(name = "inputSolutionFile")
    private List<File> inputSolutionFileList = null;

    private Boolean problemStatisticEnabled = null;

    @XmlElement(name = "problemStatisticType")
    private List<ProblemStatisticType> problemStatisticTypeList = null;

    @XmlElement(name = "singleStatisticType")
    private List<SingleStatisticType> singleStatisticTypeList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public Class<SolutionFileIO<Solution_>> getSolutionFileIOClass() {
        return solutionFileIOClass;
    }

    public void setSolutionFileIOClass(Class<SolutionFileIO<Solution_>> solutionFileIOClass) {
        this.solutionFileIOClass = solutionFileIOClass;
    }

    public Boolean getWriteOutputSolutionEnabled() {
        return writeOutputSolutionEnabled;
    }

    public void setWriteOutputSolutionEnabled(Boolean writeOutputSolutionEnabled) {
        this.writeOutputSolutionEnabled = writeOutputSolutionEnabled;
    }

    public List<File> getInputSolutionFileList() {
        return inputSolutionFileList;
    }

    public void setInputSolutionFileList(List<File> inputSolutionFileList) {
        this.inputSolutionFileList = inputSolutionFileList;
    }

    public Boolean getProblemStatisticEnabled() {
        return problemStatisticEnabled;
    }

    public void setProblemStatisticEnabled(Boolean problemStatisticEnabled) {
        this.problemStatisticEnabled = problemStatisticEnabled;
    }

    public List<ProblemStatisticType> getProblemStatisticTypeList() {
        return problemStatisticTypeList;
    }

    public void setProblemStatisticTypeList(List<ProblemStatisticType> problemStatisticTypeList) {
        this.problemStatisticTypeList = problemStatisticTypeList;
    }

    public List<SingleStatisticType> getSingleStatisticTypeList() {
        return singleStatisticTypeList;
    }

    public void setSingleStatisticTypeList(List<SingleStatisticType> singleStatisticTypeList) {
        this.singleStatisticTypeList = singleStatisticTypeList;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public void buildProblemBenchmarkList(SolverBenchmarkResult solverBenchmarkResult,
            Solution_[] extraProblems) {
        PlannerBenchmarkResult plannerBenchmarkResult = solverBenchmarkResult.getPlannerBenchmarkResult();
        List<ProblemBenchmarkResult> unifiedProblemBenchmarkResultList = plannerBenchmarkResult
                .getUnifiedProblemBenchmarkResultList();
        for (ProblemProvider<Solution_> problemProvider : buildProblemProviderList(
                solverBenchmarkResult, extraProblems)) {
            // 2 SolverBenchmarks containing equal ProblemBenchmarks should contain the same instance
            ProblemBenchmarkResult<Solution_> newProblemBenchmarkResult = buildProblemBenchmark(
                    plannerBenchmarkResult, problemProvider);
            ProblemBenchmarkResult<Solution_> problemBenchmarkResult;
            int index = unifiedProblemBenchmarkResultList.indexOf(newProblemBenchmarkResult);
            if (index < 0) {
                problemBenchmarkResult = newProblemBenchmarkResult;
                unifiedProblemBenchmarkResultList.add(problemBenchmarkResult);
            } else {
                problemBenchmarkResult = unifiedProblemBenchmarkResultList.get(index);
            }
            buildSingleBenchmark(solverBenchmarkResult, problemBenchmarkResult);
        }
    }

    private List<ProblemProvider<Solution_>> buildProblemProviderList(
            SolverBenchmarkResult solverBenchmarkResult, Solution_[] extraProblems) {
        if (ConfigUtils.isEmptyCollection(inputSolutionFileList) && extraProblems.length == 0) {
            throw new IllegalArgumentException(
                    "The solverBenchmarkResult (" + solverBenchmarkResult.getName() + ") has no problems.\n"
                            + "Maybe configure at least 1 <inputSolutionFile> directly or indirectly by inheriting it.\n"
                            + "Or maybe pass at least one problem to " + PlannerBenchmarkFactory.class.getSimpleName()
                            + ".buildPlannerBenchmark().");
        }
        List<ProblemProvider<Solution_>> problemProviderList = new ArrayList<>(
                extraProblems.length + (inputSolutionFileList == null ? 0 : inputSolutionFileList.size()));
        DefaultSolverFactory<Solution_> defaultSolverFactory =
                new DefaultSolverFactory<>(solverBenchmarkResult.getSolverConfig());
        SolutionDescriptor<Solution_> solutionDescriptor = defaultSolverFactory.buildSolutionDescriptor();
        int extraProblemIndex = 0;
        for (Solution_ extraProblem : extraProblems) {
            if (extraProblem == null) {
                throw new IllegalStateException("The benchmark problem (" + extraProblem + ") is null.");
            }
            String problemName = "Problem_" + extraProblemIndex;
            problemProviderList.add(new InstanceProblemProvider<>(problemName, solutionDescriptor, extraProblem));
            extraProblemIndex++;
        }
        if (ConfigUtils.isEmptyCollection(inputSolutionFileList)) {
            if (solutionFileIOClass != null) {
                throw new IllegalArgumentException("Cannot use solutionFileIOClass (" + solutionFileIOClass
                        + ") with an empty inputSolutionFileList (" + inputSolutionFileList + ").");
            }
        } else {
            SolutionFileIO<Solution_> solutionFileIO = buildSolutionFileIO();
            for (File inputSolutionFile : inputSolutionFileList) {
                if (!inputSolutionFile.exists()) {
                    throw new IllegalArgumentException("The inputSolutionFile (" + inputSolutionFile
                            + ") does not exist.");
                }
                problemProviderList.add(new FileProblemProvider<>(solutionFileIO, inputSolutionFile));
            }
        }
        return problemProviderList;
    }

    private SolutionFileIO<Solution_> buildSolutionFileIO() {
        if (solutionFileIOClass == null) {
            throw new IllegalArgumentException("The solutionFileIOClass (" + solutionFileIOClass + ") cannot be null.");
        }
        return ConfigUtils.newInstance(this, "solutionFileIOClass", solutionFileIOClass);
    }

    private ProblemBenchmarkResult<Solution_> buildProblemBenchmark(
            PlannerBenchmarkResult plannerBenchmarkResult, ProblemProvider<Solution_> problemProvider) {
        ProblemBenchmarkResult<Solution_> problemBenchmarkResult = new ProblemBenchmarkResult<>(plannerBenchmarkResult);
        problemBenchmarkResult.setName(problemProvider.getProblemName());
        problemBenchmarkResult.setProblemProvider(problemProvider);
        problemBenchmarkResult.setWriteOutputSolutionEnabled(
                writeOutputSolutionEnabled == null ? false : writeOutputSolutionEnabled);
        List<ProblemStatistic> problemStatisticList;
        if (BooleanUtils.isFalse(problemStatisticEnabled)) {
            if (!ConfigUtils.isEmptyCollection(problemStatisticTypeList)) {
                throw new IllegalArgumentException("The problemStatisticEnabled (" + problemStatisticEnabled
                        + ") and problemStatisticTypeList (" + problemStatisticTypeList + ") can be used together.");
            }
            problemStatisticList = Collections.emptyList();
        } else {
            List<ProblemStatisticType> problemStatisticTypeList_ = (problemStatisticTypeList == null)
                    ? Collections.singletonList(ProblemStatisticType.BEST_SCORE)
                    : problemStatisticTypeList;
            problemStatisticList = new ArrayList<>(problemStatisticTypeList_.size());
            for (ProblemStatisticType problemStatisticType : problemStatisticTypeList_) {
                problemStatisticList.add(problemStatisticType.buildProblemStatistic(problemBenchmarkResult));
            }
        }
        problemBenchmarkResult.setProblemStatisticList(problemStatisticList);
        problemBenchmarkResult.setSingleBenchmarkResultList(new ArrayList<>());
        return problemBenchmarkResult;
    }

    private void buildSingleBenchmark(SolverBenchmarkResult solverBenchmarkResult,
            ProblemBenchmarkResult problemBenchmarkResult) {
        SingleBenchmarkResult singleBenchmarkResult = new SingleBenchmarkResult(solverBenchmarkResult, problemBenchmarkResult);
        buildSubSingleBenchmarks(singleBenchmarkResult, solverBenchmarkResult.getSubSingleCount());
        for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult.getSubSingleBenchmarkResultList()) {
            subSingleBenchmarkResult.setPureSubSingleStatisticList(new ArrayList<>(
                    singleStatisticTypeList == null ? 0 : singleStatisticTypeList.size()));
        }
        if (singleStatisticTypeList != null) {
            for (SingleStatisticType singleStatisticType : singleStatisticTypeList) {
                for (SubSingleBenchmarkResult subSingleBenchmarkResult : singleBenchmarkResult
                        .getSubSingleBenchmarkResultList()) {
                    subSingleBenchmarkResult.getPureSubSingleStatisticList().add(
                            singleStatisticType.buildPureSubSingleStatistic(subSingleBenchmarkResult));
                }
            }
        }
        singleBenchmarkResult.initSubSingleStatisticMaps();
        solverBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
        problemBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
    }

    private void buildSubSingleBenchmarks(SingleBenchmarkResult parent, int subSingleCount) {
        List<SubSingleBenchmarkResult> subSingleBenchmarkResultList = new ArrayList<>(subSingleCount);
        for (int i = 0; i < subSingleCount; i++) {
            SubSingleBenchmarkResult subSingleBenchmarkResult = new SubSingleBenchmarkResult(parent, i);
            subSingleBenchmarkResultList.add(subSingleBenchmarkResult);
        }
        parent.setSubSingleBenchmarkResultList(subSingleBenchmarkResultList);
    }

    @Override
    public ProblemBenchmarksConfig<Solution_> inherit(ProblemBenchmarksConfig<Solution_> inheritedConfig) {
        solutionFileIOClass = ConfigUtils.inheritOverwritableProperty(solutionFileIOClass,
                inheritedConfig.getSolutionFileIOClass());
        writeOutputSolutionEnabled = ConfigUtils.inheritOverwritableProperty(writeOutputSolutionEnabled,
                inheritedConfig.getWriteOutputSolutionEnabled());
        inputSolutionFileList = ConfigUtils.inheritMergeableListProperty(inputSolutionFileList,
                inheritedConfig.getInputSolutionFileList());
        problemStatisticEnabled = ConfigUtils.inheritOverwritableProperty(problemStatisticEnabled,
                inheritedConfig.getProblemStatisticEnabled());
        problemStatisticTypeList = ConfigUtils.inheritMergeableListProperty(problemStatisticTypeList,
                inheritedConfig.getProblemStatisticTypeList());
        singleStatisticTypeList = ConfigUtils.inheritMergeableListProperty(singleStatisticTypeList,
                inheritedConfig.getSingleStatisticTypeList());
        return this;
    }

    @Override
    public ProblemBenchmarksConfig<Solution_> copyConfig() {
        return new ProblemBenchmarksConfig<Solution_>().inherit(this);
    }

}
