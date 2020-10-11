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

package org.optaplanner.core.config.solver;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.AbstractConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import org.optaplanner.core.config.phase.NoChangePhaseConfig;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.phase.custom.CustomPhaseConfig;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.random.RandomType;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.io.OptaPlannerXmlSerializationException;
import org.optaplanner.core.impl.io.jaxb.SolverConfigIO;
import org.optaplanner.core.impl.solver.random.RandomFactory;

/**
 * To read it from XML, use {@link #createFromXmlResource(String)}.
 * To build a {@link SolverFactory} with it, use {@link SolverFactory#create(SolverConfig)}.
 */
@XmlRootElement(name = SolverConfig.XML_ELEMENT_NAME)
@XmlType(name = SolverConfig.XML_TYPE_NAME, propOrder = {
        "environmentMode",
        "daemon",
        "randomType",
        "randomSeed",
        "randomFactoryClass",
        "moveThreadCount",
        "moveThreadBufferSize",
        "threadFactoryClass",
        "solutionClass",
        "entityClassList",
        "scoreDirectorFactoryConfig",
        "terminationConfig",
        "phaseConfigList"
})
public class SolverConfig<Solution_> extends AbstractConfig<Solution_, SolverConfig<Solution_>> {

    public static final String XML_ELEMENT_NAME = "solver";
    public static final String XML_NAMESPACE = "https://www.optaplanner.org/xsd/solver";
    public static final String XML_TYPE_NAME = "solverConfig";

    /**
     * Reads an XML solver configuration from the classpath.
     *
     * @param solverConfigResource never null, a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlResource(String solverConfigResource) {
        return createFromXmlResource(solverConfigResource, null);
    }

    /**
     * As defined by {@link #createFromXmlResource(String)}.
     *
     * @param solverConfigResource never null, a classpath resource
     *        as defined by {@link ClassLoader#getResource(String)}
     * @param classLoader sometimes null, the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlResource(String solverConfigResource,
            ClassLoader classLoader) {
        ClassLoader actualClassLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        try (InputStream in = actualClassLoader.getResourceAsStream(solverConfigResource)) {
            if (in == null) {
                String errorMessage = "The solverConfigResource (" + solverConfigResource
                        + ") does not exist as a classpath resource in the classLoader (" + actualClassLoader + ").";
                if (solverConfigResource.startsWith("/")) {
                    errorMessage += "\nA classpath resource should not start with a slash (/)."
                            + " A solverConfigResource adheres to ClassLoader.getResource(String)."
                            + " Maybe remove the leading slash from the solverConfigResource.";
                }
                throw new IllegalArgumentException(errorMessage);
            }
            return createFromXmlInputStream(in, classLoader);
        } catch (OptaPlannerXmlSerializationException e) {
            throw new IllegalArgumentException("Unmarshalling of solverConfigResource (" + solverConfigResource + ") fails.",
                    e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the solverConfigResource (" + solverConfigResource + ") fails.", e);
        }
    }

    /**
     * Reads an XML solver configuration from the file system.
     * <p>
     * Warning: this leads to platform dependent code,
     * it's recommend to use {@link #createFromXmlResource(String)} instead.
     *
     * @param solverConfigFile never null
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlFile(File solverConfigFile) {
        return createFromXmlFile(solverConfigFile, null);
    }

    /**
     * As defined by {@link #createFromXmlFile(File)}.
     *
     * @param solverConfigFile never null
     * @param classLoader sometimes null, the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlFile(File solverConfigFile,
            ClassLoader classLoader) {
        try (InputStream in = new FileInputStream(solverConfigFile)) {
            return createFromXmlInputStream(in, classLoader);
        } catch (OptaPlannerXmlSerializationException e) {
            throw new IllegalArgumentException("Unmarshalling the solverConfigFile (" + solverConfigFile + ") fails.", e);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The solverConfigFile (" + solverConfigFile + ") was not found.", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading the solverConfigFile (" + solverConfigFile + ") fails.", e);
        }
    }

    /**
     * @param in never null, gets closed
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlInputStream(InputStream in) {
        return createFromXmlInputStream(in, null);
    }

    /**
     * As defined by {@link #createFromXmlInputStream(InputStream)}.
     *
     * @param in never null, gets closed
     * @param classLoader sometimes null, the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlInputStream(InputStream in,
            ClassLoader classLoader) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return createFromXmlReader(reader, classLoader);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("This vm does not support the charset (" + StandardCharsets.UTF_8 + ").", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Reading solverConfigInputStream fails.", e);
        }
    }

    /**
     * @param reader never null, gets closed
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlReader(Reader reader) {
        return createFromXmlReader(reader, null);
    }

    /**
     * As defined by {@link #createFromXmlReader(Reader)}.
     *
     * @param reader never null, gets closed
     * @param classLoader sometimes null, the {@link ClassLoader} to use for loading all resources and {@link Class}es,
     *        null to use the default {@link ClassLoader}
     * @return never null
     */
    public static <Solution_> SolverConfig<Solution_> createFromXmlReader(Reader reader, ClassLoader classLoader) {
        SolverConfigIO<Solution_> solverConfigIO = new SolverConfigIO<>();
        Object solverConfigObject = solverConfigIO.read(reader);

        if (!(solverConfigObject instanceof SolverConfig)) {
            throw new IllegalArgumentException("The " + SolverConfig.class.getSimpleName()
                    + "'s XML root element resolves to a different type ("
                    + (solverConfigObject == null ? null : solverConfigObject.getClass().getSimpleName()));
        }
        SolverConfig<Solution_> solverConfig = (SolverConfig<Solution_>) solverConfigObject;
        solverConfig.setClassLoader(classLoader);
        return solverConfig;
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    public static final String MOVE_THREAD_COUNT_NONE = "NONE";
    public static final String MOVE_THREAD_COUNT_AUTO = "AUTO";

    @XmlTransient
    private ClassLoader classLoader = null;

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    protected EnvironmentMode environmentMode = null;
    protected Boolean daemon = null;
    protected RandomType randomType = null;
    protected Long randomSeed = null;
    protected Class<? extends RandomFactory> randomFactoryClass = null;
    protected String moveThreadCount = null;
    protected Integer moveThreadBufferSize = null;
    protected Class<? extends ThreadFactory> threadFactoryClass = null;

    protected Class<?> solutionClass = null;
    @XmlElement(name = "entityClass")
    protected List<Class<?>> entityClassList = null;

    @XmlElement(name = "scoreDirectorFactory")
    protected ScoreDirectorFactoryConfig<Solution_> scoreDirectorFactoryConfig = null;

    @XmlElement(name = "termination")
    private TerminationConfig<Solution_> terminationConfig;

    @XmlElements({
            @XmlElement(name = ConstructionHeuristicPhaseConfig.XML_ELEMENT_NAME,
                    type = ConstructionHeuristicPhaseConfig.class),
            @XmlElement(name = CustomPhaseConfig.XML_ELEMENT_NAME, type = CustomPhaseConfig.class),
            @XmlElement(name = ExhaustiveSearchPhaseConfig.XML_ELEMENT_NAME, type = ExhaustiveSearchPhaseConfig.class),
            @XmlElement(name = LocalSearchPhaseConfig.XML_ELEMENT_NAME, type = LocalSearchPhaseConfig.class),
            @XmlElement(name = NoChangePhaseConfig.XML_ELEMENT_NAME, type = NoChangePhaseConfig.class),
            @XmlElement(name = PartitionedSearchPhaseConfig.XML_ELEMENT_NAME, type = PartitionedSearchPhaseConfig.class)
    })
    protected List<PhaseConfig> phaseConfigList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    /**
     * Create an empty solver config.
     */
    public SolverConfig() {
    }

    /**
     * @param classLoader sometimes null
     */
    public SolverConfig(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Allows you to programmatically change the {@link SolverConfig} per concurrent request,
     * based on a template solver config,
     * by building a separate {@link SolverFactory} with {@link SolverFactory#create(SolverConfig)}
     * and a separate {@link Solver} per request to avoid race conditions.
     *
     * @param inheritedConfig never null
     */
    public SolverConfig(SolverConfig<Solution_> inheritedConfig) {
        inherit(inheritedConfig);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    public void setEnvironmentMode(EnvironmentMode environmentMode) {
        this.environmentMode = environmentMode;
    }

    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }

    public RandomType getRandomType() {
        return randomType;
    }

    public void setRandomType(RandomType randomType) {
        this.randomType = randomType;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public Class<? extends RandomFactory> getRandomFactoryClass() {
        return randomFactoryClass;
    }

    public void setRandomFactoryClass(Class<? extends RandomFactory> randomFactoryClass) {
        this.randomFactoryClass = randomFactoryClass;
    }

    public String getMoveThreadCount() {
        return moveThreadCount;
    }

    public void setMoveThreadCount(String moveThreadCount) {
        this.moveThreadCount = moveThreadCount;
    }

    public Integer getMoveThreadBufferSize() {
        return moveThreadBufferSize;
    }

    public void setMoveThreadBufferSize(Integer moveThreadBufferSize) {
        this.moveThreadBufferSize = moveThreadBufferSize;
    }

    public Class<? extends ThreadFactory> getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    public Class<?> getSolutionClass() {
        return solutionClass;
    }

    public void setSolutionClass(Class<?> solutionClass) {
        this.solutionClass = solutionClass;
    }

    public List<Class<?>> getEntityClassList() {
        return entityClassList;
    }

    public void setEntityClassList(List<Class<?>> entityClassList) {
        this.entityClassList = entityClassList;
    }

    public ScoreDirectorFactoryConfig<Solution_> getScoreDirectorFactoryConfig() {
        return scoreDirectorFactoryConfig;
    }

    public void setScoreDirectorFactoryConfig(ScoreDirectorFactoryConfig<Solution_> scoreDirectorFactoryConfig) {
        this.scoreDirectorFactoryConfig = scoreDirectorFactoryConfig;
    }

    public TerminationConfig<Solution_> getTerminationConfig() {
        return terminationConfig;
    }

    public void setTerminationConfig(TerminationConfig<Solution_> terminationConfig) {
        this.terminationConfig = terminationConfig;
    }

    public List<PhaseConfig> getPhaseConfigList() {
        return phaseConfigList;
    }

    public void setPhaseConfigList(List<PhaseConfig> phaseConfigList) {
        this.phaseConfigList = phaseConfigList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public SolverConfig<Solution_> withEnvironmentMode(EnvironmentMode environmentMode) {
        this.environmentMode = environmentMode;
        return this;
    }

    public SolverConfig<Solution_> withDaemon(Boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public SolverConfig<Solution_> withRandomType(RandomType randomType) {
        this.randomType = randomType;
        return this;
    }

    public SolverConfig<Solution_> withRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    public SolverConfig<Solution_> withRandomFactoryClass(Class<? extends RandomFactory> randomFactoryClass) {
        this.randomFactoryClass = randomFactoryClass;
        return this;
    }

    public SolverConfig<Solution_> withMoveThreadCount(String moveThreadCount) {
        this.moveThreadCount = moveThreadCount;
        return this;
    }

    public SolverConfig<Solution_> withMoveThreadBufferSize(Integer moveThreadBufferSize) {
        this.moveThreadBufferSize = moveThreadBufferSize;
        return this;
    }

    public SolverConfig<Solution_> withThreadFactoryClass(Class<? extends ThreadFactory> threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
        return this;
    }

    public SolverConfig<Solution_> withSolutionClass(Class<?> solutionClass) {
        this.solutionClass = solutionClass;
        return this;
    }

    public SolverConfig<Solution_> withEntityClassList(List<Class<?>> entityClassList) {
        this.entityClassList = entityClassList;
        return this;
    }

    public SolverConfig<Solution_> withEntityClasses(Class<?>... entityClasses) {
        this.entityClassList = Arrays.asList(entityClasses);
        return this;
    }

    public SolverConfig<Solution_> withScoreDirectorFactory(ScoreDirectorFactoryConfig<Solution_> scoreDirectorFactoryConfig) {
        this.scoreDirectorFactoryConfig = scoreDirectorFactoryConfig;
        return this;
    }

    public SolverConfig<Solution_> withTerminationConfig(TerminationConfig<Solution_> terminationConfig) {
        this.terminationConfig = terminationConfig;
        return this;
    }

    public SolverConfig<Solution_> withPhaseList(List<PhaseConfig> phaseConfigList) {
        this.phaseConfigList = phaseConfigList;
        return this;
    }

    public SolverConfig<Solution_> withPhases(PhaseConfig... phaseConfigs) {
        this.phaseConfigList = Arrays.asList(phaseConfigs);
        return this;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    public EnvironmentMode determineEnvironmentMode() {
        return defaultIfNull(environmentMode, EnvironmentMode.REPRODUCIBLE);
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    public void offerRandomSeedFromSubSingleIndex(long subSingleIndex) {
        if (environmentMode == null || environmentMode.isReproducible()) {
            if (randomFactoryClass == null && randomSeed == null) {
                randomSeed = subSingleIndex;
            }
        }
    }

    /**
     * Do not use this method, it is an internal method.
     * Use {@link #SolverConfig(SolverConfig)} instead.
     *
     * @param inheritedConfig never null
     */
    @Override
    public SolverConfig<Solution_> inherit(SolverConfig<Solution_> inheritedConfig) {
        classLoader = ConfigUtils.inheritOverwritableProperty(classLoader, inheritedConfig.getClassLoader());
        environmentMode = ConfigUtils.inheritOverwritableProperty(environmentMode, inheritedConfig.getEnvironmentMode());
        daemon = ConfigUtils.inheritOverwritableProperty(daemon, inheritedConfig.getDaemon());
        randomType = ConfigUtils.inheritOverwritableProperty(randomType, inheritedConfig.getRandomType());
        randomSeed = ConfigUtils.inheritOverwritableProperty(randomSeed, inheritedConfig.getRandomSeed());
        randomFactoryClass = ConfigUtils.inheritOverwritableProperty(
                randomFactoryClass, inheritedConfig.getRandomFactoryClass());
        moveThreadCount = ConfigUtils.inheritOverwritableProperty(moveThreadCount,
                inheritedConfig.getMoveThreadCount());
        moveThreadBufferSize = ConfigUtils.inheritOverwritableProperty(moveThreadBufferSize,
                inheritedConfig.getMoveThreadBufferSize());
        threadFactoryClass = ConfigUtils.inheritOverwritableProperty(threadFactoryClass,
                inheritedConfig.getThreadFactoryClass());
        solutionClass = ConfigUtils.inheritOverwritableProperty(solutionClass, inheritedConfig.getSolutionClass());
        entityClassList = ConfigUtils.inheritMergeableListProperty(
                entityClassList, inheritedConfig.getEntityClassList());
        scoreDirectorFactoryConfig = ConfigUtils.inheritConfig(scoreDirectorFactoryConfig,
                inheritedConfig.getScoreDirectorFactoryConfig());
        terminationConfig = ConfigUtils.inheritConfig(terminationConfig, inheritedConfig.getTerminationConfig());
        phaseConfigList = ConfigUtils.inheritMergeableListConfig(phaseConfigList,
                inheritedConfig.getPhaseConfigList());
        return this;
    }

    @Override
    public SolverConfig<Solution_> copyConfig() {
        return new SolverConfig<Solution_>().inherit(this);
    }

}
