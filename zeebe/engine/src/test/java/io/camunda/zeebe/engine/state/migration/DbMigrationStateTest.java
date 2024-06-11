/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.migration;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.engine.state.mutable.MutableProcessingState;
import io.camunda.zeebe.engine.util.ProcessingStateExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ProcessingStateExtension.class)
public class DbMigrationStateTest {
  private MutableProcessingState processingState;

  @Test
  void shouldHaveNoVersionUntilWritten() {
    // given
    final var sut = processingState.getMigrationState();

    // when + then
    assertThat(sut.getMigratedByVersion()).isNull();
  }

  @Test
  void shouldWriteVersionToState() {
    // given
    final var sut = processingState.getMigrationState();

    // when
    sut.setMigratedByVersion("1.2.3");

    // then
    assertThat(sut.getMigratedByVersion()).isEqualTo("1.2.3");
  }
}
