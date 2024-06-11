/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.client.api;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.topology.TopologyUpdateNotifier.TopologyUpdateListener;
import io.camunda.zeebe.topology.state.ClusterTopology;

public interface BrokerTopologyManager extends TopologyUpdateListener {

  /**
   * Returns live topology that includes which brokers are available, who is leader for each
   * partition, etc.
   */
  BrokerClusterState getTopology();

  /**
   * Returns the current cluster topology. The topology contains the information about brokers which
   * are part of the cluster, and the partition distribution. Unlike {@link BrokerClusterState} this
   * also includes information about brokers which are currently unreachable.
   */
  ClusterTopology getClusterTopology();

  /**
   * Adds the topology listener. For each existing brokers, the listener will be notified via {@link
   * BrokerTopologyListener#brokerAdded(MemberId)}. After that, the listener gets notified of every
   * new broker added or removed events.
   *
   * @param listener the topology listener
   */
  void addTopologyListener(final BrokerTopologyListener listener);

  /**
   * Removes the given topology listener by identity.
   *
   * @param listener the listener to remove
   */
  void removeTopologyListener(final BrokerTopologyListener listener);
}
