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

package org.optaplanner.core.impl.score.stream.drools;

import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.impl.score.holder.AbstractScoreHolder;
import org.optaplanner.core.impl.score.stream.ConstraintSession;

public class DroolsConstraintSession<Solution_, Score_ extends Score<Score_>>
        implements ConstraintSession<Solution_, Score_> {

    private final KieSession kieSession;
    private final AbstractScoreHolder<Score_> scoreHolder;

    public DroolsConstraintSession(KieSession kieSession, AbstractScoreHolder<Score_> scoreHolder) {
        this.kieSession = kieSession;
        this.scoreHolder = scoreHolder;
    }

    @Override
    public void insert(Object fact) {
        kieSession.insert(fact);
    }

    @Override
    public void update(Object fact, String variableName) {
        FactHandle factHandle = kieSession.getFactHandle(fact);
        if (variableName == null) {
            kieSession.update(factHandle, fact);
        } else {
            kieSession.update(factHandle, fact, variableName);
        }
    }

    @Override
    public void retract(Object fact) {
        FactHandle factHandle = kieSession.getFactHandle(fact);
        kieSession.delete(factHandle);
    }

    @Override
    public Score_ calculateScore(int initScore) {
        kieSession.fireAllRules();
        return scoreHolder.extractScore(initScore);
    }

    @Override
    public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
        kieSession.fireAllRules();
        return scoreHolder.getConstraintMatchTotalMap();
    }

    @Override
    public Map<Object, Indictment<Score_>> getIndictmentMap() {
        kieSession.fireAllRules();
        return scoreHolder.getIndictmentMap();
    }

    @Override
    public void close() {
        kieSession.dispose();
    }

}
