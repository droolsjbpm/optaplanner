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

package org.optaplanner.core.impl.score.stream.drools.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.drools.model.Drools;
import org.drools.model.RuleItemBuilder;
import org.drools.model.view.ViewItem;
import org.optaplanner.core.impl.score.inliner.BigDecimalWeightedScoreImpacter;
import org.optaplanner.core.impl.score.inliner.IntWeightedScoreImpacter;
import org.optaplanner.core.impl.score.inliner.LongWeightedScoreImpacter;
import org.optaplanner.core.impl.score.stream.drools.DroolsConstraint;

import static org.drools.model.PatternDSL.rule;

/**
 * Used when building a consequence to a rule.
 *
 * For rules where variables are directly used in a consequence,
 * the extra patterns (see {@link PatternVariable}) are unnecessary overhead.
 * In these cases, the left hand side will bring a simplified instance of this class.
 * In all other cases, the rule context will reference the extra pattern variables.
 *
 * <p>
 * Consider the following simple rule, in DRL-like pseudocode:
 *
 * <pre>
 * {@code
 *  rule "Simple rule"
 *  when
 *      accumulate(
 *          Something(),
 *          $count: count()
 *      )
 *  then
 *      // Do something with $count
 *  end
 * }
 * </pre>
 *
 * In this case, the consequence can use the variable "count" directly. However, consider the following rule, where
 * we also want to filter on the "count" variable:
 *
 * <pre>
 * {@code
 *  rule "Simple rule"
 *  when
 *      accumulate(
 *          Something(),
 *          $count: count()
 *      )
 *      $newA: Integer(this == $count, this > 0)
 *  then
 *      // Do something with $newA after we know it is greater than 0.
 *  end
 * }
 * </pre>
 *
 * In this case, the extra pattern variable "newA" is required,
 * because we want to have a pattern on which to apply the filter.
 * The same goes for joining etc.
 * Whenever the variable is not passed directly into a consequence,
 * the use of this class needs to be replaced by the use of {@link PatternVariable}.
 */
abstract class AbstractRuleContext {

    private final List<ViewItem<?>> viewItems;

    protected AbstractRuleContext(ViewItem<?>... viewItems) {
        this.viewItems = Arrays.stream(viewItems).collect(Collectors.toList());
    }

    protected static void impactScore(DroolsConstraint<?> constraint, Drools drools,
            IntWeightedScoreImpacter scoreImpacter, int impact, Object... justifications) {
        constraint.assertCorrectImpact(impact);
        // TODO do the actual impact
    }

    protected static void impactScore(DroolsConstraint<?> constraint, Drools drools,
            LongWeightedScoreImpacter scoreImpacter, long impact, Object... justifications) {
        constraint.assertCorrectImpact(impact);
        // TODO do the actual impact
    }

    protected static void impactScore(DroolsConstraint<?> constraint, Drools drools,
            BigDecimalWeightedScoreImpacter scoreImpacter, BigDecimal impact, Object... justifications) {
        constraint.assertCorrectImpact(impact);
        // TODO do the actual impact
    }

    protected <Solution_> RuleBuilder<Solution_> assemble(ConsequenceBuilder<Solution_> consequenceBuilder) {
        return (constraint, scoreImpacter) -> {
            List<RuleItemBuilder<?>> ruleItemBuilderList = new ArrayList<>(viewItems);
            ruleItemBuilderList.add(consequenceBuilder.apply(constraint, scoreImpacter));
            return rule(constraint.getConstraintPackage(), constraint.getConstraintName())
                    .build(ruleItemBuilderList.toArray(new RuleItemBuilder[0]));
        };
    }

}
