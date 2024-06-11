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
import io.atomix.primitive.partition.PartitionId;
import io.camunda.zeebe.test.util.asserts.EitherAssert;
import io.camunda.zeebe.topology.state.ClusterTopology;
import io.camunda.zeebe.topology.util.RoundRobinPartitionDistributor;
import io.camunda.zeebe.topology.util.TopologyUtil;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.ShrinkingMode;
import net.jqwik.api.constraints.IntRange;

class ScaleRequestTransformerTest {
  @Property(tries = 10)
  void shouldScaleAndReassignWithReplicationFactor1(
      @ForAll @IntRange(min = 1, max = 100) final int partitionCount,
      @ForAll @IntRange(min = 1, max = 100) final int oldClusterSize,
      @ForAll @IntRange(min = 1, max = 100) final int newClusterSize) {
    shouldScaleAndReassign(partitionCount, 1, oldClusterSize, newClusterSize);
  }

  @Property(tries = 10)
  void shouldScaleAndReassignWithReplicationFactor2(
      @ForAll @IntRange(min = 1, max = 100) final int partitionCount,
      @ForAll @IntRange(min = 2, max = 100) final int oldClusterSize,
      @ForAll @IntRange(min = 2, max = 100) final int newClusterSize) {
    shouldScaleAndReassign(partitionCount, 2, oldClusterSize, newClusterSize);
  }

  @Property(tries = 10, shrinking = ShrinkingMode.OFF, edgeCases = EdgeCasesMode.NONE)
  void shouldScaleAndReassignWithReplicationFactor3(
      @ForAll @IntRange(min = 1, max = 100) final int partitionCount,
      @ForAll @IntRange(min = 3, max = 100) final int oldClusterSize,
      @ForAll @IntRange(min = 3, max = 100) final int newClusterSize) {
    shouldScaleAndReassign(partitionCount, 3, oldClusterSize, newClusterSize);
  }

  @Property(tries = 10)
  void shouldScaleAndReassignWithReplicationFactor4(
      @ForAll @IntRange(min = 1, max = 100) final int partitionCount,
      @ForAll @IntRange(min = 4, max = 100) final int oldClusterSize,
      @ForAll @IntRange(min = 4, max = 100) final int newClusterSize) {
    shouldScaleAndReassign(partitionCount, 4, oldClusterSize, newClusterSize);
  }

  @Property
  void shouldFailIfClusterSizeLessThanReplicationFactor3(
      @ForAll @IntRange(min = 0, max = 2) final int newClusterSize) {
    shouldFailIfClusterSizeLessThanReplicationFactor(3, 3, 3, newClusterSize);
  }

  @Property
  void shouldFailIfClusterSizeLessThanReplicationFactor4(
      @ForAll @IntRange(min = 0, max = 3) final int newClusterSize) {
    shouldFailIfClusterSizeLessThanReplicationFactor(12, 4, 6, newClusterSize);
  }

  void shouldFailIfClusterSizeLessThanReplicationFactor(
      final int partitionCount,
      final int replicationFactor,
      final int oldClusterSize,
      final int newClusterSize) {
    // given
    final var oldDistribution =
        new RoundRobinPartitionDistributor()
            .distributePartitions(
                getClusterMembers(oldClusterSize),
                getSortedPartitionIds(partitionCount),
                replicationFactor);
    final var oldClusterTopology = TopologyUtil.getClusterTopologyFrom(oldDistribution);

    //  when
    final var operationsEither =
        new ScaleRequestTransformer(getClusterMembers(newClusterSize))
            .operations(oldClusterTopology);

    // then
    EitherAssert.assertThat(operationsEither)
        .isLeft()
        .left()
        .isInstanceOf(TopologyRequestFailedException.InvalidRequest.class);
  }

  void shouldScaleAndReassign(
      final int partitionCount,
      final int replicationFactor,
      final int oldClusterSize,
      final int newClusterSize) {
    // given
    final var expectedNewDistribution =
        new RoundRobinPartitionDistributor()
            .distributePartitions(
                getClusterMembers(newClusterSize),
                getSortedPartitionIds(partitionCount),
                replicationFactor);

    final var oldDistribution =
        new RoundRobinPartitionDistributor()
            .distributePartitions(
                getClusterMembers(oldClusterSize),
                getSortedPartitionIds(partitionCount),
                replicationFactor);
    final var oldClusterTopology = TopologyUtil.getClusterTopologyFrom(oldDistribution);

    // when
    final var operations =
        new ScaleRequestTransformer(getClusterMembers(newClusterSize))
            .operations(oldClusterTopology)
            .get();

    // apply operations to generate new topology
    final ClusterTopology newTopology =
        TestTopologyChangeSimulator.apply(oldClusterTopology, operations);

    // then
    final var newDistribution = TopologyUtil.getPartitionDistributionFrom(newTopology, "temp");
    assertThat(newDistribution).isEqualTo(expectedNewDistribution);
    assertThat(newTopology.members().keySet())
        .containsExactlyInAnyOrderElementsOf(getClusterMembers(newClusterSize));
  }

  private List<PartitionId> getSortedPartitionIds(final int partitionCount) {
    return IntStream.rangeClosed(1, partitionCount)
        .mapToObj(id -> PartitionId.from("temp", id))
        .collect(Collectors.toList());
  }

  private Set<MemberId> getClusterMembers(final int newClusterSize) {
    return IntStream.range(0, newClusterSize)
        .mapToObj(Integer::toString)
        .map(MemberId::from)
        .collect(Collectors.toSet());
  }
}
