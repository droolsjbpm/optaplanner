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

package org.optaplanner.test.impl.score.stream;

import org.optaplanner.core.api.score.Score;

public abstract class AbstractAssertion<Solution_, A extends AbstractAssertion<Solution_, A, V>, V
        extends AbstractConstraintVerifier<Solution_, A, V>> {

    private final V parentConstraintVerifier;
    private final Score<?> score;

    protected AbstractAssertion(V constraintVerifier, Score<?> actualScore) {
        this.parentConstraintVerifier = constraintVerifier;
        this.score = actualScore;
    }

    protected final V getParentConstraintVerifier() {
        return parentConstraintVerifier;
    }

    public void expectScore(Score<?> score) {
    }

}
