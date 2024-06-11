/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.job;

import static io.camunda.zeebe.test.util.record.RecordingExporter.jobRecords;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.test.util.Strings;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class JobUpdateTimeoutTest {
  @ClassRule public static final EngineRule ENGINE = EngineRule.singlePartition();
  private static final String PROCESS_ID = "process";
  private static String jobType;

  @Rule
  public final RecordingExporterTestWatcher recordingExporterTestWatcher =
      new RecordingExporterTestWatcher();

  @Before
  public void setup() {
    jobType = Strings.newRandomValidBpmnId();
  }

  @Test
  public void shouldIncreaseJobTimeout() {
    // given
    ENGINE.createJob(jobType, PROCESS_ID);
    final var batchRecord =
        ENGINE.jobs().withType(jobType).withTimeout(Duration.ofMinutes(5).toMillis()).activate();
    final JobRecordValue job = batchRecord.getValue().getJobs().get(0);
    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long timeout = Duration.ofMinutes(10).toMillis();

    // when
    final var updatedRecord = ENGINE.job().withKey(jobKey).withTimeout(timeout).updateTimeout();

    // then
    assertJobDeadline(updatedRecord, jobKey, job, timeout);
  }

  @Test
  public void shouldDecreaseJobTimeout() {
    // given
    ENGINE.createJob(jobType, PROCESS_ID);
    final var batchRecord =
        ENGINE.jobs().withType(jobType).withTimeout(Duration.ofMinutes(15).toMillis()).activate();
    final JobRecordValue job = batchRecord.getValue().getJobs().get(0);
    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long timeout = Duration.ofMinutes(10).toMillis();

    // when
    final var updatedRecord = ENGINE.job().withKey(jobKey).withTimeout(timeout).updateTimeout();

    // then
    assertJobDeadline(updatedRecord, jobKey, job, timeout);
  }

  @Test
  public void shouldRejectUpdateTimoutIfJobNotFound() {
    // given
    final long jobKey = 123L;
    final long timeout = Duration.ofMinutes(10).toMillis();

    // when
    final var jobRecord =
        ENGINE.job().withKey(jobKey).withTimeout(timeout).expectRejection().updateTimeout();

    // then
    Assertions.assertThat(jobRecord)
        .hasRejectionType(RejectionType.NOT_FOUND)
        .hasRejectionReason(
            "Expected to update job deadline with key '%d', but no such job was found"
                .formatted(jobKey));
  }

  @Test
  public void shouldRejectUpdateTimoutIfDeadlineNotFound() {
    // given
    final Record<JobRecordValue> job = ENGINE.createJob(jobType, PROCESS_ID);
    final long timeout = Duration.ofMinutes(10).toMillis();

    // when
    final var jobRecord =
        ENGINE.job().withKey(job.getKey()).withTimeout(timeout).expectRejection().updateTimeout();

    // then
    Assertions.assertThat(jobRecord)
        .hasRejectionType(RejectionType.INVALID_STATE)
        .hasRejectionReason(
            "Expected to update the timeout of job with key '%d', but it is not active"
                .formatted(job.getKey()));
  }

  @Test
  public void shouldIncreaseJobTimeoutSecondTime() {
    // given
    ENGINE.createJob(jobType, PROCESS_ID);
    final var batchRecord =
        ENGINE.jobs().withType(jobType).withTimeout(Duration.ofMinutes(5).toMillis()).activate();
    final JobRecordValue job = batchRecord.getValue().getJobs().get(0);
    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long firstTimeout = Duration.ofMinutes(10).toMillis();
    final long secondTimeout = Duration.ofMinutes(20).toMillis();

    // when
    ENGINE.job().withKey(jobKey).withTimeout(firstTimeout).updateTimeout();
    final var updatedRecord =
        ENGINE.job().withKey(jobKey).withTimeout(secondTimeout).updateTimeout();

    // then
    assertJobDeadline(updatedRecord, jobKey, job, secondTimeout);
  }

  @Test
  public void shouldTimeOutAfterDecreasingTimeout() {
    // given
    ENGINE.createJob(jobType, PROCESS_ID);
    final var batchRecord =
        ENGINE.jobs().withType(jobType).withTimeout(Duration.ofMinutes(10).toMillis()).activate();
    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long timeout = Duration.ofMinutes(5).toMillis();

    // when
    ENGINE.job().withKey(jobKey).withTimeout(timeout).updateTimeout();
    ENGINE.increaseTime(Duration.ofMinutes(6));

    // then
    final List<Record<JobRecordValue>> jobEvents =
        jobRecords().withType(jobType).limit(4).collect(Collectors.toList());

    assertThat(jobEvents).extracting(Record::getKey).contains(jobKey);
    assertThat(jobEvents)
        .extracting(Record::getIntent)
        .containsExactly(
            JobIntent.CREATED, JobIntent.TIMEOUT_UPDATED, JobIntent.TIME_OUT, JobIntent.TIMED_OUT);
  }

  @Test
  public void shouldTimeOutAfterIncreasingTimeout() {
    // given
    ENGINE.createJob(jobType, PROCESS_ID);
    final var batchRecord =
        ENGINE.jobs().withType(jobType).withTimeout(Duration.ofMinutes(10).toMillis()).activate();
    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long timeout = Duration.ofMinutes(15).toMillis();

    // when
    ENGINE.job().withKey(jobKey).withTimeout(timeout).updateTimeout();
    ENGINE.increaseTime(Duration.ofMinutes(16));

    // then
    final List<Record<JobRecordValue>> jobEvents =
        jobRecords().withType(jobType).limit(4).collect(Collectors.toList());

    assertThat(jobEvents).extracting(Record::getKey).contains(jobKey);
    assertThat(jobEvents)
        .extracting(Record::getIntent)
        .containsExactly(
            JobIntent.CREATED, JobIntent.TIMEOUT_UPDATED, JobIntent.TIME_OUT, JobIntent.TIMED_OUT);
  }

  @Test
  public void shouldUpdateJobTimeoutForCustomTenant() {
    // given
    final String tenantId = "acme";
    ENGINE.createJob(jobType, PROCESS_ID, Collections.emptyMap(), tenantId);

    final var batchRecord =
        ENGINE
            .jobs()
            .withType(jobType)
            .withTimeout(Duration.ofMinutes(5).toMillis())
            .withTenantId(tenantId)
            .activate();

    final JobRecordValue job = batchRecord.getValue().getJobs().get(0);
    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long timeout = Duration.ofMinutes(10).toMillis();

    // when
    final Record<JobRecordValue> updatedRecord =
        ENGINE
            .job()
            .withKey(jobKey)
            .withTimeout(timeout)
            .withAuthorizedTenantIds(tenantId)
            .updateTimeout();

    // then
    Assertions.assertThat(updatedRecord.getValue()).hasTenantId(tenantId);
    assertJobDeadline(updatedRecord, jobKey, job, timeout);
  }

  @Test
  public void shouldRejectUpdateRetriesIfTenantIsUnauthorized() {
    // given
    final String tenantId = "acme";
    final String falseTenantId = "foo";
    ENGINE.createJob(jobType, PROCESS_ID, Collections.emptyMap(), tenantId);

    final var batchRecord =
        ENGINE
            .jobs()
            .withType(jobType)
            .withTimeout(Duration.ofMinutes(5).toMillis())
            .withTenantId(tenantId)
            .activate();

    final long jobKey = batchRecord.getValue().getJobKeys().get(0);
    final long timeout = Duration.ofMinutes(10).toMillis();

    // when
    final Record<JobRecordValue> jobRecord =
        ENGINE
            .job()
            .withKey(jobKey)
            .withTimeout(timeout)
            .withAuthorizedTenantIds(falseTenantId)
            .expectRejection()
            .updateTimeout();

    // then
    Assertions.assertThat(jobRecord).hasRejectionType(RejectionType.NOT_FOUND);
  }

  private static void assertJobDeadline(
      final Record<JobRecordValue> updatedRecord,
      final long jobKey,
      final JobRecordValue job,
      final long timeout) {
    Assertions.assertThat(updatedRecord)
        .hasRecordType(RecordType.EVENT)
        .hasIntent(JobIntent.TIMEOUT_UPDATED);
    assertThat(updatedRecord.getKey()).isEqualTo(jobKey);

    assertThat(updatedRecord.getValue().getDeadline()).isNotEqualTo(job.getDeadline());

    assertThat(updatedRecord.getValue().getDeadline())
        .isCloseTo(
            ENGINE.getClock().getCurrentTimeInMillis() + timeout,
            within(Duration.ofMillis(100).toMillis()));
  }
}
