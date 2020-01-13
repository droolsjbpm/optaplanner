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
package org.optaplanner.core.api.function;

/**
 * Represents a function that accepts five arguments and produces an int-valued result.
 * This is the {@code int}-producing primitive specialization for {@link PentaFunction}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #applyAsInt(Object, Object, Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 * @param <D> the type of the fourth argument to the function
 * @param <E> the type of the fifth argument to the function
 *
 * @see PentaFunction
 */
@FunctionalInterface
public interface ToIntPentaFunction<A, B, C, D, E> {

    /**
     * Applies this function to the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the fourth function argument
     * @return the function result
     */
    int applyAsInt(A a, B b, C c, D d, E e);
}
