/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.qa.util.cluster;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.broker.shared.BrokerConfiguration.BrokerProperties;
import io.camunda.zeebe.broker.system.configuration.BrokerCfg;
import io.camunda.zeebe.qa.util.actuator.HealthActuator;
import io.camunda.zeebe.restore.RestoreApp;
import io.camunda.zeebe.shared.Profile;
import java.util.function.Consumer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Represents an instance of the {@link RestoreApp} Spring application. */
public final class TestRestoreApp extends TestSpringApplication<TestRestoreApp> {
  private final BrokerProperties config;
  private Long backupId;

  public TestRestoreApp() {
    this(new BrokerProperties());
  }

  public TestRestoreApp(final BrokerProperties config) {
    super(RestoreApp.class);
    this.config = config;

    //noinspection resource
    withBean("config", config, BrokerProperties.class).withAdditionalProfile(Profile.RESTORE);
  }

  @Override
  public TestRestoreApp self() {
    return this;
  }

  @Override
  public MemberId nodeId() {
    return MemberId.from(String.valueOf(config.getCluster().getNodeId()));
  }

  @Override
  public HealthActuator healthActuator() {
    return new HealthActuator.NoopHealthActuator();
  }

  @Override
  public boolean isGateway() {
    return false;
  }

  @Override
  protected String[] commandLineArgs() {
    return backupId == null ? super.commandLineArgs() : new String[] {"--backupId=" + backupId};
  }

  @Override
  protected SpringApplicationBuilder createSpringBuilder() {
    return super.createSpringBuilder().web(WebApplicationType.NONE);
  }

  public TestRestoreApp withBrokerConfig(final Consumer<BrokerCfg> modifier) {
    modifier.accept(config);
    return this;
  }

  public TestRestoreApp withBackupId(final long backupId) {
    this.backupId = backupId;
    return this;
  }
}