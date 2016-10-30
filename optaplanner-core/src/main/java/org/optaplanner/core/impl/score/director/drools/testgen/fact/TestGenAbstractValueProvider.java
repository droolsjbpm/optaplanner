/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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
package org.optaplanner.core.impl.score.director.drools.testgen.fact;

abstract class TestGenAbstractValueProvider<T> implements TestGenValueProvider<T> {

    protected final T value;

    public TestGenAbstractValueProvider(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public T getUninitialized() {
        return null;
    }

    @Override
    public void printSetup(StringBuilder sb) {
        // no setup required
    }

}
