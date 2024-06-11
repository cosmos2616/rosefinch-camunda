/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.stream.impl;

/**
 * The user command is rejected because RecordProcessor failed to process the command record and
 * failed to handle the processing error gracefully.
 */
public class CommandRejectionException extends RuntimeException {

  public CommandRejectionException(final String message) {
    super(message);
  }
}
