/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.exporter.opensearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IndexPolicyResponse(
    @JsonProperty("updated_indices") int updatedIndices,
    boolean failures,
    @JsonProperty("failed_indices") List<FailedIndex> failedIndices) {
  record FailedIndex(
      @JsonProperty("index_name") String name,
      @JsonProperty("index_uuid") String uuid,
      String reason) {}
}
