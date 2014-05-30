package org.optaplanner.core.impl.testdata.domain.chained.mappedby;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public interface TestdataMappedByChainedObject {

    /**
     * @return sometimes null
     */
    @PlanningShadowVariable(mappedBy = "chainedObject")
    TestdataMappedByChainedEntity getNextEntity();
    void setNextEntity(TestdataMappedByChainedEntity nextEntity);

}
