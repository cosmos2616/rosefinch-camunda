/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.topology.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.topology.api.TopologyChangeResponse;
import io.camunda.zeebe.topology.api.TopologyManagementRequest.AddMembersRequest;
import io.camunda.zeebe.topology.api.TopologyManagementRequest.JoinPartitionRequest;
import io.camunda.zeebe.topology.api.TopologyManagementRequest.LeavePartitionRequest;
import io.camunda.zeebe.topology.api.TopologyManagementRequest.ReassignPartitionsRequest;
import io.camunda.zeebe.topology.api.TopologyManagementRequest.RemoveMembersRequest;
import io.camunda.zeebe.topology.gossip.ClusterTopologyGossipState;
import io.camunda.zeebe.topology.state.ClusterTopology;
import io.camunda.zeebe.topology.state.MemberState;
import io.camunda.zeebe.topology.state.PartitionState;
import io.camunda.zeebe.topology.state.TopologyChangeOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.MemberJoinOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.MemberLeaveOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.MemberRemoveOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.PartitionChangeOperation.PartitionForceReconfigureOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.PartitionChangeOperation.PartitionJoinOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.PartitionChangeOperation.PartitionLeaveOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.PartitionChangeOperation.PartitionReconfigurePriorityOperation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class ProtoBufSerializerTest {

  final ProtoBufSerializer protoBufSerializer = new ProtoBufSerializer();

  @ParameterizedTest
  @MethodSource("provideClusterTopologies")
  void shouldEncodeAndDecode(final ClusterTopology initialClusterTopology) {
    // given
    final ClusterTopologyGossipState gossipState = new ClusterTopologyGossipState();
    gossipState.setClusterTopology(initialClusterTopology);

    // when
    final var decodedState = protoBufSerializer.decode(protoBufSerializer.encode(gossipState));

    // then
    assertThat(decodedState.getClusterTopology())
        .describedAs("Decoded clusterTopology must be equal to initial one")
        .isEqualTo(initialClusterTopology);
  }

  @Test
  void shouldEncodeAndDecodeClusterTopology() {
    // given
    final var initialClusterTopology = topologyWithTwoMembers();

    // when
    final var encoded = protoBufSerializer.encode(initialClusterTopology);
    final var decodedClusterTopology =
        protoBufSerializer.decodeClusterTopology(encoded, 0, encoded.length);

    // then
    assertThat(decodedClusterTopology)
        .describedAs("Decoded clusterTopology must be equal to initial one")
        .isEqualTo(initialClusterTopology);
  }

  @Test
  void shouldEncodeAndDecodeAddMembersRequest() {
    // given
    final var addMembersRequest =
        new AddMembersRequest(Set.of(MemberId.from("1"), MemberId.from("2")), false);

    // when
    final var encodedRequest = protoBufSerializer.encodeAddMembersRequest(addMembersRequest);

    // then
    final var decodedRequest = protoBufSerializer.decodeAddMembersRequest(encodedRequest);
    assertThat(decodedRequest).isEqualTo(addMembersRequest);
  }

  @Test
  void shouldEncodeAndDecodeRemoveMembersRequest() {
    // given
    final var removeMembersRequest =
        new RemoveMembersRequest(Set.of(MemberId.from("1"), MemberId.from("2")), false);

    // when
    final var encodedRequest = protoBufSerializer.encodeRemoveMembersRequest(removeMembersRequest);

    // then
    final var decodedRequest = protoBufSerializer.decodeRemoveMembersRequest(encodedRequest);
    assertThat(decodedRequest).isEqualTo(removeMembersRequest);
  }

  @Test
  void shouldEncodeAndDecodeReassignAllPartitionsRequest() {
    // given
    final var reassignPartitionsRequest =
        new ReassignPartitionsRequest(Set.of(MemberId.from("1"), MemberId.from("2")), false);

    // when
    final var encodedRequest =
        protoBufSerializer.encodeReassignPartitionsRequest(reassignPartitionsRequest);

    // then
    final var decodedRequest = protoBufSerializer.decodeReassignPartitionsRequest(encodedRequest);
    assertThat(decodedRequest).isEqualTo(reassignPartitionsRequest);
  }

  @Test
  void shouldEncodeAndDecodeJoinPartitionRequest() {
    // given
    final var joinPartitionRequest = new JoinPartitionRequest(MemberId.from("2"), 3, 5, false);

    // when
    final var encodedRequest = protoBufSerializer.encodeJoinPartitionRequest(joinPartitionRequest);

    // then
    final var decodedRequest = protoBufSerializer.decodeJoinPartitionRequest(encodedRequest);
    assertThat(decodedRequest).isEqualTo(joinPartitionRequest);
  }

  @Test
  void shouldEncodeAndDecodeLeavePartitionRequest() {
    // given
    final var leavePartitionRequest = new LeavePartitionRequest(MemberId.from("6"), 2, false);

    // when
    final var encodedRequest =
        protoBufSerializer.encodeLeavePartitionRequest(leavePartitionRequest);

    // then
    final var decodedRequest = protoBufSerializer.decodeLeavePartitionRequest(encodedRequest);
    assertThat(decodedRequest).isEqualTo(leavePartitionRequest);
  }

  @Test
  void shouldEncodeAndDecodeTopologyChangeResponse() {
    // given
    final var topologyChangeResponse =
        new TopologyChangeResponse(
            2,
            Map.of(
                MemberId.from("1"),
                MemberState.initializeAsActive(Map.of()),
                MemberId.from("2"),
                MemberState.initializeAsActive(Map.of())),
            Map.of(MemberId.from("2"), MemberState.initializeAsActive(Map.of())),
            List.of(
                new MemberLeaveOperation(MemberId.from("1")),
                new PartitionJoinOperation(MemberId.from("2"), 1, 2)));

    // when
    final var encodedResponse = protoBufSerializer.encodeResponse(topologyChangeResponse);

    // then
    final var decodedResponse =
        protoBufSerializer.decodeTopologyChangeResponse(encodedResponse).get();
    assertThat(decodedResponse).isEqualTo(topologyChangeResponse);
  }

  private static Stream<ClusterTopology> provideClusterTopologies() {
    return Stream.of(
        topologyWithOneMemberNoPartitions(),
        topologyWithOneJoiningMember(),
        topologyWithOneLeavingMember(),
        topologyWithOneLeftMember(),
        topologyWithOneMemberOneActivePartition(),
        topologyWithOneMemberOneLeavingPartition(),
        topologyWithOneMemberOneJoiningPartition(),
        topologyWithOneMemberTwoPartitions(),
        topologyWithTwoMembers(),
        topologyWithClusterChangePlan(),
        topologyWithCompletedClusterChangePlan(),
        topologyWithClusterChangePlanWithMemberOperations());
  }

  private static ClusterTopology topologyWithOneMemberNoPartitions() {
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()));
  }

  private static ClusterTopology topologyWithOneJoiningMember() {
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.uninitialized().toJoining());
  }

  private static ClusterTopology topologyWithOneLeavingMember() {
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()).toLeaving());
  }

  private static ClusterTopology topologyWithOneLeftMember() {
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()).toLeft());
  }

  private static ClusterTopology topologyWithOneMemberOneActivePartition() {
    return ClusterTopology.init()
        .addMember(
            MemberId.from("0"),
            MemberState.initializeAsActive(Map.of(1, PartitionState.active(1))));
  }

  private static ClusterTopology topologyWithOneMemberOneLeavingPartition() {
    return ClusterTopology.init()
        .addMember(
            MemberId.from("0"),
            MemberState.initializeAsActive(Map.of(1, PartitionState.active(1).toLeaving())));
  }

  private static ClusterTopology topologyWithOneMemberOneJoiningPartition() {
    return ClusterTopology.init()
        .addMember(
            MemberId.from("0"),
            MemberState.initializeAsActive(Map.of(1, PartitionState.joining(1))));
  }

  private static ClusterTopology topologyWithOneMemberTwoPartitions() {
    return ClusterTopology.init()
        .addMember(
            MemberId.from("0"),
            MemberState.initializeAsActive(
                Map.of(1, PartitionState.active(1), 2, PartitionState.active(2).toLeaving())));
  }

  private static ClusterTopology topologyWithTwoMembers() {
    return ClusterTopology.init()
        .addMember(
            MemberId.from("0"),
            MemberState.initializeAsActive(
                Map.of(1, PartitionState.joining(1), 2, PartitionState.active(2))))
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()).toLeaving());
  }

  private static ClusterTopology topologyWithClusterChangePlan() {
    final List<TopologyChangeOperation> changes =
        List.of(
            new PartitionLeaveOperation(MemberId.from("1"), 1),
            new PartitionJoinOperation(MemberId.from("2"), 2, 5),
            new PartitionReconfigurePriorityOperation(MemberId.from("3"), 4, 3),
            new PartitionForceReconfigureOperation(
                MemberId.from("4"), 5, List.of(MemberId.from("1"), MemberId.from("3"))),
            new MemberRemoveOperation(MemberId.from("5"), MemberId.from("6")));
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()))
        .startTopologyChange(changes);
  }

  private static ClusterTopology topologyWithCompletedClusterChangePlan() {
    final List<TopologyChangeOperation> changes =
        List.of(new PartitionLeaveOperation(MemberId.from("1"), 1));
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()))
        .startTopologyChange(changes)
        .advanceTopologyChange(topology -> topology);
  }

  private static ClusterTopology topologyWithClusterChangePlanWithMemberOperations() {
    final List<TopologyChangeOperation> changes =
        List.of(
            new MemberJoinOperation(MemberId.from("2")),
            new MemberLeaveOperation(MemberId.from("1")));
    return ClusterTopology.init()
        .addMember(MemberId.from("1"), MemberState.initializeAsActive(Map.of()))
        .startTopologyChange(changes);
  }
}
