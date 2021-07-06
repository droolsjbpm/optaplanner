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

package org.optaplanner.benchmark.impl.statistic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.event.BestSolutionChangedEvent;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.optaplanner.core.config.solver.metric.SolverMetric;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.solver.DefaultSolver;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;

public class StatisticRegistry extends StepMeterRegistry implements SolverEventListener {

    List<Consumer<Long>> stepMeterListenerList = new ArrayList<>();
    List<Consumer<Long>> bestSolutionMeterListenerList = new ArrayList<>();
    long lastUpdatedTimestamp = Long.MIN_VALUE;
    long bestSolutionChangedTimestamp = Long.MIN_VALUE;
    ScoreDefinition scoreDefinition;

    public StatisticRegistry(DefaultSolver solver) {
        super(new StepRegistryConfig() {
            @Override
            public Duration step() {
                return Duration.ofMillis(10L);
            }

            @Override
            public String prefix() {
                return "optaplanner-benchmark-statistic";
            }

            @Override
            public String get(String key) {
                return null;
            }
        }, new Clock() {
            @Override
            public long wallTime() {
                return solver.getTimeMillisSpent();
            }

            @Override
            public long monotonicTime() {
                return System.nanoTime();
            }
        });
        scoreDefinition = solver.getSolverScope().getScoreDefinition();
    }

    public void addListener(SolverMetric metric, Consumer<Long> listener) {
        if (isMetricBestSolutionBased(metric)) {
            bestSolutionMeterListenerList.add(listener);
        } else {
            stepMeterListenerList.add(listener);
        }
    }

    private boolean isMetricBestSolutionBased(SolverMetric metric) {
        switch (metric) {
            case BEST_SCORE:
            case BEST_SOLUTION_MUTATION:
            case CONSTRAINT_MATCH_TOTAL_BEST_SCORE:
            case PICKED_MOVE_TYPE_BEST_SCORE_DIFF:
                return true;
            case MEMORY_USE:
            case STEP_SCORE:
            case ERROR_COUNT:
            case SOLVE_LENGTH:
            case MOVE_COUNT_PER_STEP:
            case SCORE_CALCULATION_SPEED:
            case PICKED_MOVE_TYPE_STEP_SCORE_DIFF:
            case CONSTRAINT_MATCH_TOTAL_STEP_SCORE:
                return false;
            default:
                throw new IllegalStateException("SolverMetric (" + metric + ") does not have a case");
        }
    }

    public Set<Meter.Id> getMeterIds(SolverMetric metric, Tags runId) {
        return Search.in(this).name(name -> name.startsWith(metric.getMeterId())).tags(runId)
                .meters().stream().map(meter -> meter.getId())
                .collect(Collectors.toSet());
    }

    public Score extractScoreFromMeters(SolverMetric metric, Tags runId) {
        String[] labelNames = scoreDefinition.getLevelLabels();
        Number[] levelNumbers = new Number[labelNames.length];
        for (int i = 0; i < labelNames.length; i++) {
            levelNumbers[i] = this.find(metric.getMeterId() + "." + labelNames[i]).tags(runId).gauge().value();
        }
        return scoreDefinition.fromLevelNumbers(0, levelNumbers);
    }

    public Number getGaugeValue(SolverMetric metric, Tags runId) {
        return getGaugeValue(metric.getMeterId(), runId);
    }

    public Number getGaugeValue(String meterId, Tags runId) {
        return this.find(meterId).tags(runId).gauge().value();
    }

    @Override
    protected void publish() {
        stepMeterListenerList.forEach(listener -> listener.accept(clock.wallTime()));
        if (lastUpdatedTimestamp < bestSolutionChangedTimestamp) {
            bestSolutionMeterListenerList.forEach(listener -> listener.accept(bestSolutionChangedTimestamp));
            lastUpdatedTimestamp = bestSolutionChangedTimestamp;
        }
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public void bestSolutionChanged(BestSolutionChangedEvent event) {
        bestSolutionChangedTimestamp = event.getTimeMillisSpent();
    }
}
