/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.client.api;

import io.camunda.zeebe.broker.client.impl.RoundRobinDispatchStrategy;

/** Implementations must be thread-safe. */
public interface RequestDispatchStrategy {

  /**
   * @return {@link BrokerClusterState#PARTITION_ID_NULL} if no partition can be determined
   */
  int determinePartition(final BrokerTopologyManager topologyManager);

  /**
   * Returns a dispatch strategy which will perform a stateful round robin between the partitions.
   */
  static RequestDispatchStrategy roundRobin() {
    return new RoundRobinDispatchStrategy();
  }
}
