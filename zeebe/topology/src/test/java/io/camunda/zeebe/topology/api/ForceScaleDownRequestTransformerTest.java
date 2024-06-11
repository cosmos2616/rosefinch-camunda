/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.topology.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.test.util.asserts.EitherAssert;
import io.camunda.zeebe.topology.api.TopologyRequestFailedException.InvalidRequest;
import io.camunda.zeebe.topology.state.ClusterTopology;
import io.camunda.zeebe.topology.state.MemberState;
import io.camunda.zeebe.topology.state.PartitionState;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.MemberRemoveOperation;
import io.camunda.zeebe.topology.state.TopologyChangeOperation.PartitionChangeOperation.PartitionForceReconfigureOperation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ForceScaleDownRequestTransformerTest {
  private final MemberId id0 = MemberId.from("0");
  private final MemberId id1 = MemberId.from("1");
  private final MemberId id2 = MemberId.from("2");
  private final MemberId id3 = MemberId.from("3");

  private final ClusterTopology currentTopology =
      ClusterTopology.init()
          .addMember(id0, MemberState.initializeAsActive(Map.of()))
          .addMember(id1, MemberState.initializeAsActive(Map.of()))
          .addMember(id2, MemberState.initializeAsActive(Map.of()))
          .addMember(id3, MemberState.initializeAsActive(Map.of()))
          .updateMember(id0, m -> m.addPartition(1, PartitionState.active(1)))
          .updateMember(id1, m -> m.addPartition(1, PartitionState.active(2)))
          .updateMember(id2, m -> m.addPartition(2, PartitionState.active(1)))
          .updateMember(id3, m -> m.addPartition(2, PartitionState.active(2)));

  @Test
  void shouldGenerateForceConfigureOperations() {
    // given
    final var membersToRetain = Set.of(MemberId.from("0"), MemberId.from("2"));
    final var forceConfigureTransformer =
        new ForceScaleDownRequestTransformer(membersToRetain, id0);

    // when
    final var result = forceConfigureTransformer.operations(currentTopology);

    // then
    EitherAssert.assertThat(result).isRight();
    assertThat(result.get())
        .hasSize(4)
        .containsExactlyInAnyOrder(
            new PartitionForceReconfigureOperation(id0, 1, List.of(id0)),
            new PartitionForceReconfigureOperation(id2, 2, List.of(id2)),
            new MemberRemoveOperation(id0, id1),
            new MemberRemoveOperation(id0, id3));
  }

  @Test
  void shouldFailWhenRetainingNonExistingMember() {
    // given
    final var membersToRetain = Set.of(MemberId.from("0"), MemberId.from("4"));
    final var forceConfigureTransformer =
        new ForceScaleDownRequestTransformer(membersToRetain, id0);

    // when
    final var result = forceConfigureTransformer.operations(currentTopology);

    // then
    EitherAssert.assertThat(result).isLeft();
    assertThat(result.getLeft()).isInstanceOf(InvalidRequest.class);
  }

  @Test
  void shouldFailWhenRetainingMemberWithNoPartitions() {
    // given
    final var membersToRetain = Set.of(MemberId.from("0"), MemberId.from("1"));
    final var forceConfigureTransformer =
        new ForceScaleDownRequestTransformer(membersToRetain, id0);

    // when
    final var result = forceConfigureTransformer.operations(currentTopology);

    // then
    EitherAssert.assertThat(result).isLeft();
    assertThat(result.getLeft()).isInstanceOf(InvalidRequest.class);
  }
}
