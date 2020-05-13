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

package org.optaplanner.core.api.score.constraint;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Compares by {@link Indictment}s based on {@link Indictment#getScore()}.
 *
 * @deprecated If you need this, it is trivial to implement via {@link Comparator#comparing(Function)}.
 */
@Deprecated
public class IndictmentScoreComparator implements Comparator<Indictment>, Serializable {

    @Override
    public int compare(Indictment a, Indictment b) {
        return a.getScore().compareTo(b.getScore());
    }

}
