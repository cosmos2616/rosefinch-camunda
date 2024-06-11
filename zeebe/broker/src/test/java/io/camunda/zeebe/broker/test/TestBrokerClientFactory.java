/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.test;

import static io.camunda.zeebe.gateway.impl.configuration.ConfigurationDefaults.DEFAULT_REQUEST_TIMEOUT;

import io.atomix.cluster.AtomixCluster;
import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.broker.client.impl.BrokerClientImpl;
import io.camunda.zeebe.broker.client.impl.BrokerTopologyManagerImpl;
import io.camunda.zeebe.scheduler.ActorScheduler;
import io.camunda.zeebe.scheduler.future.ActorFuture;

public final class TestBrokerClientFactory {
  public static BrokerClient createBrokerClient(
      final AtomixCluster atomixCluster, final ActorScheduler actorScheduler) {
    final var topologyManager =
        new BrokerTopologyManagerImpl(() -> atomixCluster.getMembershipService().getMembers());
    actorScheduler.submitActor(topologyManager).join();
    atomixCluster.getMembershipService().addListener(topologyManager);

    final var client =
        new BrokerClientImpl(
            DEFAULT_REQUEST_TIMEOUT,
            atomixCluster.getMessagingService(),
            atomixCluster.getEventService(),
            actorScheduler,
            topologyManager);
    client.start().forEach(ActorFuture::join);
    return client;
  }
}
