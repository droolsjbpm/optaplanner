/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.facilitylocation.domain;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;

import org.acme.facilitylocation.solver.FacilityLocationConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintConfigurationProvider;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

@PlanningSolution
public class FacilityLocationProblem {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "facilityRange")
    private List<Facility> facilities;
    @PlanningEntityCollectionProperty
    private List<DemandPoint> demandPoints;

    @PlanningScore
    private HardSoftLongScore score;
    @ConstraintConfigurationProvider
    private FacilityLocationConstraintConfiguration constraintConfiguration = new FacilityLocationConstraintConfiguration();

    private Location southWestCorner;
    private Location northEastCorner;

    public FacilityLocationProblem() {
    }

    public FacilityLocationProblem(
            List<Facility> facilities,
            List<DemandPoint> demandPoints,
            Location southWestCorner,
            Location northEastCorner) {
        this.facilities = facilities;
        this.demandPoints = demandPoints;
        this.southWestCorner = southWestCorner;
        this.northEastCorner = northEastCorner;
    }

    public static FacilityLocationProblem empty() {
        FacilityLocationProblem problem = new FacilityLocationProblem(
                emptyList(),
                emptyList(),
                new Location(-90, -180),
                new Location(90, 180));
        problem.setScore(HardSoftLongScore.ZERO);
        return problem;
    }

    public List<Facility> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<Facility> facilities) {
        this.facilities = facilities;
    }

    public List<DemandPoint> getDemandPoints() {
        return demandPoints;
    }

    public void setDemandPoints(List<DemandPoint> demandPoints) {
        this.demandPoints = demandPoints;
    }

    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }

    public FacilityLocationConstraintConfiguration getConstraintConfiguration() {
        return constraintConfiguration;
    }

    public void setConstraintConfiguration(FacilityLocationConstraintConfiguration constraintConfiguration) {
        this.constraintConfiguration = constraintConfiguration;
    }

    public List<Location> getBounds() {
        return Arrays.asList(southWestCorner, northEastCorner);
    }

    public long getTotalCost() {
        return facilities.stream()
                .filter(Facility::isUsed)
                .mapToLong(Facility::getSetupCost)
                .sum();
    }

    public long getPotentialCost() {
        return facilities.stream()
                .mapToLong(Facility::getSetupCost)
                .sum();
    }

    public String getTotalDistance() {
        long distance = demandPoints.stream()
                .filter(DemandPoint::isAssigned)
                .mapToLong(DemandPoint::distanceToFacility)
                .sum();
        return distance / 1000 + " km";
    }

    @Override
    public String toString() {
        return "FacilityLocationProblem{" +
                "facilities: " + facilities.size() +
                ", demandPoints: " + demandPoints.size() +
                ", score: " + score +
                '}';
    }
}
