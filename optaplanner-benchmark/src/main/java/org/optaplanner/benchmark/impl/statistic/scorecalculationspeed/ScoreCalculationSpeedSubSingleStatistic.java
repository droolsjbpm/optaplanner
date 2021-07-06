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

package org.optaplanner.benchmark.impl.statistic.scorecalculationspeed;

import java.util.List;

import org.optaplanner.benchmark.config.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.impl.result.SubSingleBenchmarkResult;
import org.optaplanner.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import org.optaplanner.benchmark.impl.statistic.StatisticRegistry;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.config.solver.metric.SolverMetric;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;

import io.micrometer.core.instrument.Tags;

public class ScoreCalculationSpeedSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, ScoreCalculationSpeedStatisticPoint> {

    private final long timeMillisThresholdInterval;

    public ScoreCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public ScoreCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(benchmarkResult, ProblemStatisticType.SCORE_CALCULATION_SPEED);
        if (timeMillisThresholdInterval <= 0L) {
            throw new IllegalArgumentException("The timeMillisThresholdInterval (" + timeMillisThresholdInterval
                    + ") must be bigger than 0.");
        }
        this.timeMillisThresholdInterval = timeMillisThresholdInterval;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(StatisticRegistry registry, Tags runTag, Solver<Solution_> solver) {
        registry.addListener(SolverMetric.SCORE_CALCULATION_SPEED, timestamp -> {
            pointList.add(new ScoreCalculationSpeedStatisticPoint(timestamp,
                    registry.getGaugeValue(SolverMetric.SCORE_CALCULATION_SPEED, runTag).longValue()));
        });
    }

    @Override
    public void close(StatisticRegistry registry, Tags runTag, Solver<Solution_> solver) {
    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return ScoreCalculationSpeedStatisticPoint.buildCsvLine("timeMillisSpent", "scoreCalculationSpeed");
    }

    @Override
    protected ScoreCalculationSpeedStatisticPoint createPointFromCsvLine(ScoreDefinition scoreDefinition,
            List<String> csvLine) {
        return new ScoreCalculationSpeedStatisticPoint(Long.parseLong(csvLine.get(0)),
                Long.parseLong(csvLine.get(1)));
    }

}
