/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.client.api;

public class BrokerResponseException extends BrokerClientException {

  public BrokerResponseException(final String message) {
    super(message);
  }

  public BrokerResponseException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public BrokerResponseException(final Throwable cause) {
    super(cause);
  }
}
