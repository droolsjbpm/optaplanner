/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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
package org.optaplanner.core.impl.testdata.util;

import java.util.ArrayList;
import java.util.Collection;

public class TestdataCodeAssertableArrayList<E> extends ArrayList<E> implements CodeAssertable {

    private static final long serialVersionUID = -6085607048567865778L;

    private final String code;

    public TestdataCodeAssertableArrayList(String code, Collection<? extends E> c) {
        super(c);
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

}
