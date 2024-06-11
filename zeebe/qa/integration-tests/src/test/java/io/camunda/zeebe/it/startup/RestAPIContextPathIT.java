/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.it.startup;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.cluster.TestZeebePort;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import java.time.Duration;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

@ZeebeIntegration
public final class RestAPIContextPathIT {
  @TestZeebe
  private final TestStandaloneBroker zeebe =
      new TestStandaloneBroker().withProperty("spring.webflux.base-path", "/zeebe");

  @Test
  void shouldConnectWithContextPath() {
    // given
    //noinspection resource
    final var client =
        zeebe
            .newClientBuilder()
            .preferRestOverGrpc(true)
            .restAddress(zeebe.uri("http", TestZeebePort.REST, "zeebe"))
            .build();

    // when
    final Future<Topology> topology = client.newTopologyRequest().send();

    // then
    assertThat(topology).succeedsWithin(Duration.ofSeconds(10));
  }
}
