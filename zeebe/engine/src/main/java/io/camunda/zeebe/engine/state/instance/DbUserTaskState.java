/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.instance;

import io.camunda.zeebe.auth.impl.Authorization;
import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbForeignKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.engine.state.immutable.UserTaskState;
import io.camunda.zeebe.engine.state.mutable.MutableUserTaskState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.usertask.UserTaskRecord;
import java.util.List;
import java.util.Map;

public class DbUserTaskState implements UserTaskState, MutableUserTaskState {

  // key => user task record value
  // we need two separate wrapper to not interfere with get and put
  // see https://github.com/zeebe-io/zeebe/issues/1914
  private final UserTaskRecordValue userTaskRecordToRead = new UserTaskRecordValue();
  private final UserTaskRecordValue userTaskRecordToWrite = new UserTaskRecordValue();

  private final DbLong userTaskKey;

  private final ColumnFamily<DbLong, UserTaskRecordValue> userTasksColumnFamily;

  // key => job state
  private final DbForeignKey<DbLong> fkUserTask;
  private final UserTaskLifecycleStateValue userTaskState = new UserTaskLifecycleStateValue();
  private final ColumnFamily<DbForeignKey<DbLong>, UserTaskLifecycleStateValue>
      statesUserTaskColumnFamily;

  public DbUserTaskState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    userTaskKey = new DbLong();
    fkUserTask = new DbForeignKey<>(userTaskKey, ZbColumnFamilies.USER_TASKS);

    userTasksColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.USER_TASKS, transactionContext, userTaskKey, userTaskRecordToRead);

    statesUserTaskColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.USER_TASK_STATES, transactionContext, fkUserTask, userTaskState);
  }

  @Override
  public void create(final UserTaskRecord userTask) {
    userTaskKey.wrapLong(userTask.getUserTaskKey());
    // do not persist variables in user task state
    userTaskRecordToWrite.setRecordWithoutVariables(userTask);
    userTasksColumnFamily.insert(userTaskKey, userTaskRecordToWrite);
    // initialize state
    userTaskState.setLifecycleState(LifecycleState.CREATING);
    statesUserTaskColumnFamily.insert(fkUserTask, userTaskState);
  }

  @Override
  public void update(final UserTaskRecord userTask) {
    userTaskKey.wrapLong(userTask.getUserTaskKey());
    // do not persist variables in user task state
    userTaskRecordToWrite.setRecordWithoutVariables(userTask);
    userTasksColumnFamily.update(userTaskKey, userTaskRecordToWrite);
  }

  @Override
  public void updateUserTaskLifecycleState(final long key, final LifecycleState newLifecycleState) {
    userTaskKey.wrapLong(key);
    userTaskState.setLifecycleState(newLifecycleState);
    statesUserTaskColumnFamily.update(fkUserTask, userTaskState);
  }

  @Override
  public void delete(final long key) {
    userTaskKey.wrapLong(key);
    userTasksColumnFamily.deleteExisting(userTaskKey);
    statesUserTaskColumnFamily.deleteExisting(fkUserTask);
  }

  @Override
  public LifecycleState getLifecycleState(final long key) {
    userTaskKey.wrapLong(key);
    final UserTaskLifecycleStateValue storedLifecycleState =
        statesUserTaskColumnFamily.get(fkUserTask);
    if (storedLifecycleState == null) {
      return LifecycleState.NOT_FOUND;
    }
    return storedLifecycleState.getLifecycleState();
  }

  @Override
  public UserTaskRecord getUserTask(final long key) {
    userTaskKey.wrapLong(key);
    final UserTaskRecordValue userTask = userTasksColumnFamily.get(userTaskKey);
    return userTask == null ? null : userTask.getRecord();
  }

  @Override
  public UserTaskRecord getUserTask(final long key, final Map<String, Object> authorizations) {
    final UserTaskRecord userTask = getUserTask(key);
    if (userTask != null
        && getAuthorizedTenantIds(authorizations).contains(userTask.getTenantId())) {
      return userTask;
    }
    return null;
  }

  private List<String> getAuthorizedTenantIds(final Map<String, Object> authorizations) {
    return (List<String>) authorizations.get(Authorization.AUTHORIZED_TENANTS);
  }
}
