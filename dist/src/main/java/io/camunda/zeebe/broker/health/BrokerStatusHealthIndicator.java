/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.health;

import io.camunda.zeebe.broker.SpringBrokerBridge;
import io.camunda.zeebe.broker.system.monitoring.BrokerHealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public final class BrokerStatusHealthIndicator implements HealthIndicator {

  private static final Health HEALTHY = Health.up().build();
  private static final Health UNHEALTHY = Health.down().build();

  private final SpringBrokerBridge brokerBridge;

  @Autowired
  public BrokerStatusHealthIndicator(final SpringBrokerBridge brokerBridge) {
    this.brokerBridge = brokerBridge;
  }

  @Override
  public Health health() {
    final var isHealthy =
        brokerBridge
            .getBrokerHealthCheckService()
            .map(BrokerHealthCheckService::isBrokerHealthy)
            .orElse(false);

    return isHealthy ? HEALTHY : UNHEALTHY;
  }
}
