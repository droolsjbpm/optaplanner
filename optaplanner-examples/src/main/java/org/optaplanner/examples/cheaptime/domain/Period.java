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

package org.optaplanner.examples.cheaptime.domain;

import org.optaplanner.examples.common.domain.AbstractPersistable;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("CtPeriod")
public class Period extends AbstractPersistable implements Comparable<Period> {

    private final int period;
    private long powerPriceMicros;

    public Period(int id, long powerPriceMicros) {
        super(id);
        this.period = id;
        this.powerPriceMicros = powerPriceMicros;
    }

    public int getPeriod() {
        return period;
    }

    public long getPowerPriceMicros() {
        return powerPriceMicros;
    }

    public void setPowerPriceMicros(long powerPriceMicros) {
        this.powerPriceMicros = powerPriceMicros;
    }

    @Override
    public int compareTo(Period o) {
        return Long.compare(this.period, o.period);
    }
}
