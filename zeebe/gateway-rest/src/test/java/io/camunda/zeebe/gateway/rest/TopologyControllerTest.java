/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.gateway.rest;

import io.camunda.zeebe.broker.client.api.BrokerClient;
import io.camunda.zeebe.broker.client.api.BrokerClusterState;
import io.camunda.zeebe.broker.client.api.BrokerTopologyManager;
import io.camunda.zeebe.gateway.protocol.rest.BrokerInfo;
import io.camunda.zeebe.gateway.protocol.rest.Partition;
import io.camunda.zeebe.gateway.protocol.rest.Partition.HealthEnum;
import io.camunda.zeebe.gateway.protocol.rest.Partition.RoleEnum;
import io.camunda.zeebe.gateway.protocol.rest.TopologyResponse;
import io.camunda.zeebe.gateway.rest.TopologyControllerTest.TestTopologyApplication;
import io.camunda.zeebe.protocol.record.PartitionHealthStatus;
import io.camunda.zeebe.util.VersionUtil;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    classes = {TestTopologyApplication.class, TopologyController.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class TopologyControllerTest {

  private static final String TOPOLOGY_BASE_URL = "v1/topology";

  @MockBean BrokerClient brokerClient;
  @MockBean BrokerTopologyManager topologyManager;

  @Autowired private WebTestClient webClient;

  @BeforeEach
  void setUp() {
    Mockito.when(brokerClient.getTopologyManager()).thenReturn(topologyManager);
  }

  @Test
  public void shouldGetTopology() {
    // given
    final var version = VersionUtil.getVersion();
    final var expectedResponse =
        new TopologyResponse()
            .gatewayVersion(version)
            .clusterSize(3)
            .partitionsCount(1)
            .replicationFactor(3)
            .addBrokersItem(createBroker(version, 0, RoleEnum.LEADER, HealthEnum.HEALTHY))
            .addBrokersItem(createBroker(version, 1, RoleEnum.FOLLOWER, HealthEnum.HEALTHY))
            .addBrokersItem(createBroker(version, 2, RoleEnum.INACTIVE, HealthEnum.UNHEALTHY));
    final var brokerClusterState = new TestBrokerClusterState(version);
    Mockito.when(brokerClient.getTopologyManager().getTopology()).thenReturn(brokerClusterState);

    // when / then
    webClient
        .get()
        .uri(TOPOLOGY_BASE_URL)
        .headers(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TopologyResponse.class)
        .isEqualTo(expectedResponse);
  }

  @Test
  void shouldReturnEmptyTopology() {
    // given
    final var version = VersionUtil.getVersion();
    final var expectedResponse = new TopologyResponse().gatewayVersion(version);
    Mockito.when(brokerClient.getTopologyManager().getTopology()).thenReturn(null);

    // when / then
    webClient
        .get()
        .uri(TOPOLOGY_BASE_URL)
        .headers(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TopologyResponse.class)
        .isEqualTo(expectedResponse);
  }

  private BrokerInfo createBroker(
      final String version, final int nodeId, final RoleEnum role, final HealthEnum health) {
    final var partition = new Partition().partitionId(1).health(health).role(role);

    return new BrokerInfo()
        .nodeId(nodeId)
        .host("localhost")
        .port(26501 + nodeId)
        .version(version)
        .addPartitionsItem(partition);
  }

  @SpringBootApplication
  static class TestTopologyApplication {}

  /**
   * Topology stub which returns a static topology with 3 brokers, 1 partition, replication factor
   * 3, where 0 is the leader (healthy), 1 is the follower (healthy), and 2 is inactive.
   */
  private record TestBrokerClusterState(String version) implements BrokerClusterState {

    @Override
    public int getClusterSize() {
      return 3;
    }

    @Override
    public int getPartitionsCount() {
      return 1;
    }

    @Override
    public int getReplicationFactor() {
      return 3;
    }

    @Override
    public int getLeaderForPartition(final int partition) {
      return 0;
    }

    @Override
    public Set<Integer> getFollowersForPartition(final int partition) {
      return Set.of(1);
    }

    @Override
    public Set<Integer> getInactiveNodesForPartition(final int partition) {
      return Set.of(2);
    }

    @Override
    public int getRandomBroker() {
      return ThreadLocalRandom.current().nextInt(0, 3);
    }

    @Override
    public List<Integer> getPartitions() {
      return List.of(1);
    }

    @Override
    public List<Integer> getBrokers() {
      return List.of(0, 1, 2);
    }

    @Override
    public String getBrokerAddress(final int brokerId) {
      return "localhost:" + (26501 + brokerId);
    }

    @Override
    public int getPartition(final int index) {
      return 1;
    }

    @Override
    public String getBrokerVersion(final int brokerId) {
      return version;
    }

    @Override
    public PartitionHealthStatus getPartitionHealth(final int brokerId, final int partition) {
      if (partition != 1) {
        return PartitionHealthStatus.NULL_VAL;
      }

      return switch (brokerId) {
        case 0, 1 -> PartitionHealthStatus.HEALTHY;
        case 2 -> PartitionHealthStatus.UNHEALTHY;
        default -> PartitionHealthStatus.NULL_VAL;
      };
    }
  }
}
