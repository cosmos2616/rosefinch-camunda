/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.model.bpmn.builder;

import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.EscalationEventDefinition;
import io.camunda.zeebe.model.bpmn.instance.Event;

public abstract class AbstractEscalationEventDefinitionBuilder<
        B extends AbstractEscalationEventDefinitionBuilder<B>>
    extends AbstractRootElementBuilder<B, EscalationEventDefinition> {

  public AbstractEscalationEventDefinitionBuilder(
      final BpmnModelInstance modelInstance,
      final EscalationEventDefinition element,
      final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  @Override
  public B id(final String identifier) {
    return super.id(identifier);
  }

  /** Sets the escalation attribute with escalationCode. */
  public B escalationCode(final String escalationCode) {
    element.setEscalation(findEscalationForCode(escalationCode));
    return myself;
  }

  /**
   * Finishes the building of a escalation event definition.
   *
   * @param <T>
   * @return the parent event builder
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public <T extends AbstractFlowNodeBuilder> T escalationEventDefinitionDone() {
    return (T) ((Event) element.getParentElement()).builder();
  }
}
