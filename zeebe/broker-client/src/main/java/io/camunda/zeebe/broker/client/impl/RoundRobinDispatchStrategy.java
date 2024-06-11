/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.client.impl;

import io.camunda.zeebe.broker.client.api.BrokerClusterState;
import io.camunda.zeebe.broker.client.api.BrokerTopologyManager;
import io.camunda.zeebe.broker.client.api.RequestDispatchStrategy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Return the next partition using a round-robin strategy, but skips the partitions where there is
 * no leader at the moment.
 */
public final class RoundRobinDispatchStrategy implements RequestDispatchStrategy {

  private final AtomicInteger partitions = new AtomicInteger(0);

  @Override
  public int determinePartition(final BrokerTopologyManager topologyManager) {
    final BrokerClusterState topology = topologyManager.getTopology();

    if (topology != null) {
      for (int i = 0; i < topology.getPartitionsCount(); i++) {
        final int offset = partitions.getAndIncrement();
        final int partition = topology.getPartition(offset);
        if (topology.getLeaderForPartition(partition) != BrokerClusterState.NODE_ID_NULL) {
          return partition;
        }
      }
    }

    return BrokerClusterState.PARTITION_ID_NULL;
  }
}
