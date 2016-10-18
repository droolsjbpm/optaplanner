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

package org.optaplanner.persistence.jackson.api.score;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.optaplanner.core.api.score.Score;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractScoreJacksonJsonSerializerAndDeserializerTest {

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    protected <S extends Score, W extends TestScoreWrapper<S>> void assertSerializeAndDeserialize(S expectedScore, W input) {
        String jsonString;
        W output;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writeValueAsString(input);
            output = (W) objectMapper.readValue(jsonString, input.getClass());
        } catch (IOException e) {
            throw new IllegalStateException("Marshalling or unmarshalling for input (" + input + ") failed.", e);
        }
        assertThat(output.getScore()).isEqualTo(expectedScore);
        String regex;
        if (expectedScore != null) {
            regex = "\\{\\s*" // Start of element
                    + "\"score\":\""
                    + expectedScore.toString().replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]") // Score
                    + "\""
                    + "\\s*\\}"; // End of element
        } else {
            regex = "\\{\"score\":null\\}"; // Start and end of element
        }
        assertThat(jsonString).matches(Pattern.compile(regex));
    }

    public static abstract class TestScoreWrapper<S extends Score> implements Serializable {

        public abstract S getScore();

    }

}
