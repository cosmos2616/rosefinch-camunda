/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.immutable;

import io.camunda.zeebe.engine.state.compensation.CompensationSubscription;
import java.util.List;
import java.util.Optional;

public interface CompensationSubscriptionState {

  CompensationSubscription get(String tenantId, long processInstanceKey, long key);

  List<CompensationSubscription> findSubscriptionsByProcessInstanceKey(
      String tenantId, long processInstanceKey);

  Optional<CompensationSubscription> findSubscriptionByCompensationHandlerId(
      String tenantId, long processInstanceKey, String compensationHandlerId);

  List<CompensationSubscription> findSubscriptionsByThrowEventInstanceKey(
      String tenantId, long processInstanceKey, long throwEventInstanceKey);
}
