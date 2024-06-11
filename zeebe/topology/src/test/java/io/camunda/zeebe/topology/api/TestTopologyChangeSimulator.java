/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.topology.api;

import static org.assertj.core.api.Assertions.fail;

import io.camunda.zeebe.topology.changes.NoopPartitionChangeExecutor;
import io.camunda.zeebe.topology.changes.NoopTopologyMembershipChangeExecutor;
import io.camunda.zeebe.topology.changes.TopologyChangeAppliersImpl;
import io.camunda.zeebe.topology.state.ClusterTopology;
import io.camunda.zeebe.topology.state.TopologyChangeOperation;
import java.util.List;

final class TestTopologyChangeSimulator {

  static ClusterTopology apply(
      final ClusterTopology currentTopology, final List<TopologyChangeOperation> operations) {
    final var topologyChangeSimulator =
        new TopologyChangeAppliersImpl(
            new NoopPartitionChangeExecutor(), new NoopTopologyMembershipChangeExecutor());
    ClusterTopology newTopology = currentTopology;
    if (!operations.isEmpty()) {
      newTopology = currentTopology.startTopologyChange(operations);
    }
    while (newTopology.hasPendingChanges()) {
      final var operation = newTopology.nextPendingOperation();
      final var applier = topologyChangeSimulator.getApplier(operation);
      final var init = applier.init(newTopology);
      if (init.isLeft()) {
        fail("Failed to init operation ", init.getLeft());
      }
      newTopology = init.get().apply(newTopology);
      newTopology = newTopology.advanceTopologyChange(applier.apply().join());
    }
    return newTopology;
  }
}
