/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.protocol.impl.record.value.usertask;

import static io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord.PROP_PROCESS_BPMN_PROCESS_ID;
import static io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord.PROP_PROCESS_INSTANCE_KEY;
import static io.camunda.zeebe.util.buffer.BufferUtil.bufferAsString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.zeebe.msgpack.property.ArrayProperty;
import io.camunda.zeebe.msgpack.property.DocumentProperty;
import io.camunda.zeebe.msgpack.property.IntegerProperty;
import io.camunda.zeebe.msgpack.property.LongProperty;
import io.camunda.zeebe.msgpack.property.PackedProperty;
import io.camunda.zeebe.msgpack.property.StringProperty;
import io.camunda.zeebe.msgpack.spec.MsgPackHelper;
import io.camunda.zeebe.msgpack.value.StringValue;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.impl.record.UnifiedRecordValue;
import io.camunda.zeebe.protocol.record.value.TenantOwned;
import io.camunda.zeebe.protocol.record.value.UserTaskRecordValue;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public final class UserTaskRecord extends UnifiedRecordValue implements UserTaskRecordValue {

  public static final DirectBuffer NO_HEADERS = new UnsafeBuffer(MsgPackHelper.EMTPY_OBJECT);

  public static final String CANDIDATE_GROUPS = "candidateGroupsList";
  public static final String CANDIDATE_USERS = "candidateUsersList";
  public static final String DUE_DATE = "dueDate";
  public static final String FOLLOW_UP_DATE = "followUpDate";

  private static final String EMPTY_STRING = "";
  private static final StringValue CANDIDATE_GROUPS_VALUE = new StringValue(CANDIDATE_GROUPS);
  private static final StringValue CANDIDATE_USERS_VALUE = new StringValue(CANDIDATE_USERS);
  private static final StringValue DUE_DATE_VALUE = new StringValue(DUE_DATE);
  private static final StringValue FOLLOW_UP_DATE_VALUE = new StringValue(FOLLOW_UP_DATE);

  private final LongProperty userTaskKeyProp = new LongProperty("userTaskKey", -1);
  private final StringProperty assigneeProp = new StringProperty("assignee", EMPTY_STRING);
  private final ArrayProperty<StringValue> candidateGroupsListProp =
      new ArrayProperty<>(CANDIDATE_GROUPS, StringValue::new);
  private final ArrayProperty<StringValue> candidateUsersListProp =
      new ArrayProperty<>(CANDIDATE_USERS, StringValue::new);
  private final StringProperty dueDateProp = new StringProperty(DUE_DATE, EMPTY_STRING);
  private final StringProperty followUpDateProp = new StringProperty(FOLLOW_UP_DATE, EMPTY_STRING);
  private final LongProperty formKeyProp = new LongProperty("formKey", -1);
  private final StringProperty externalFormReferenceProp =
      new StringProperty("externalFormReference", EMPTY_STRING);

  private final DocumentProperty variableProp = new DocumentProperty("variables");
  private final PackedProperty customHeadersProp = new PackedProperty("customHeaders", NO_HEADERS);

  private final LongProperty processInstanceKeyProp =
      new LongProperty(PROP_PROCESS_INSTANCE_KEY, -1L);
  private final StringProperty bpmnProcessIdProp =
      new StringProperty(PROP_PROCESS_BPMN_PROCESS_ID, EMPTY_STRING);
  private final IntegerProperty processDefinitionVersionProp =
      new IntegerProperty("processDefinitionVersion", -1);
  private final LongProperty processDefinitionKeyProp =
      new LongProperty("processDefinitionKey", -1L);
  private final StringProperty elementIdProp = new StringProperty("elementId", EMPTY_STRING);
  private final LongProperty elementInstanceKeyProp = new LongProperty("elementInstanceKey", -1L);
  private final StringProperty tenantIdProp =
      new StringProperty("tenantId", TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final ArrayProperty<StringValue> changedAttributesProp =
      new ArrayProperty<>("changedAttributes", StringValue::new);
  private final StringProperty actionProp = new StringProperty("action", EMPTY_STRING);
  private final LongProperty creationTimestampProp = new LongProperty("creationTimestamp", -1L);

  public UserTaskRecord() {
    super(20);
    declareProperty(userTaskKeyProp)
        .declareProperty(assigneeProp)
        .declareProperty(candidateGroupsListProp)
        .declareProperty(candidateUsersListProp)
        .declareProperty(dueDateProp)
        .declareProperty(followUpDateProp)
        .declareProperty(formKeyProp)
        .declareProperty(externalFormReferenceProp)
        .declareProperty(variableProp)
        .declareProperty(customHeadersProp)
        .declareProperty(bpmnProcessIdProp)
        .declareProperty(processDefinitionVersionProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processInstanceKeyProp)
        .declareProperty(elementIdProp)
        .declareProperty(elementInstanceKeyProp)
        .declareProperty(tenantIdProp)
        .declareProperty(changedAttributesProp)
        .declareProperty(actionProp)
        .declareProperty(creationTimestampProp);
  }

  public void wrapWithoutVariables(final UserTaskRecord record) {
    userTaskKeyProp.setValue(record.getUserTaskKey());
    assigneeProp.setValue(record.getAssigneeBuffer());
    setCandidateGroupsList(record.getCandidateGroupsList());
    setCandidateUsersList(record.getCandidateUsersList());
    dueDateProp.setValue(record.getDueDateBuffer());
    followUpDateProp.setValue(record.getFollowUpDateBuffer());
    formKeyProp.setValue(record.getFormKey());
    externalFormReferenceProp.setValue(record.getExternalFormReferenceBuffer());
    final DirectBuffer customHeaders = record.getCustomHeadersBuffer();
    customHeadersProp.setValue(customHeaders, 0, customHeaders.capacity());
    bpmnProcessIdProp.setValue(record.getBpmnProcessIdBuffer());
    processDefinitionVersionProp.setValue(record.getProcessDefinitionVersion());
    processDefinitionKeyProp.setValue(record.getProcessDefinitionKey());
    processInstanceKeyProp.setValue(record.getProcessInstanceKey());
    elementIdProp.setValue(record.getElementIdBuffer());
    elementInstanceKeyProp.setValue(record.getElementInstanceKey());
    tenantIdProp.setValue(record.getTenantIdBuffer());
    creationTimestampProp.setValue(record.getCreationTimestamp());
    setChangedAttributesProp(record.getChangedAttributesProp());
    actionProp.setValue(record.getActionBuffer());
  }

  public void wrapChangedAttributes(
      final UserTaskRecord record, final boolean includeTrackingProperties) {
    record.getChangedAttributesProp().stream()
        .forEach(attribute -> updateAttribute(attribute, record));
    if (includeTrackingProperties) {
      setChangedAttributesProp(record.getChangedAttributesProp());
    }
  }

  private void updateAttribute(final StringValue attribute, final UserTaskRecord record) {
    switch (bufferAsString(attribute.getValue())) {
      case CANDIDATE_GROUPS:
        setCandidateGroupsList(record.getCandidateGroupsList());
        break;
      case CANDIDATE_USERS:
        setCandidateUsersList(record.getCandidateUsersList());
        break;
      case DUE_DATE:
        dueDateProp.setValue(record.getDueDateBuffer());
        break;
      case FOLLOW_UP_DATE:
        followUpDateProp.setValue(record.getFollowUpDateBuffer());
        break;
      default:
        break;
    }
  }

  @Override
  public long getUserTaskKey() {
    return userTaskKeyProp.getValue();
  }

  @Override
  public String getAssignee() {
    return bufferAsString(assigneeProp.getValue());
  }

  @Override
  public List<String> getCandidateGroupsList() {
    return StreamSupport.stream(candidateGroupsListProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getCandidateUsersList() {
    return StreamSupport.stream(candidateUsersListProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  @Override
  public String getDueDate() {
    return bufferAsString(dueDateProp.getValue());
  }

  @Override
  public String getFollowUpDate() {
    return bufferAsString(followUpDateProp.getValue());
  }

  @Override
  public long getFormKey() {
    return formKeyProp.getValue();
  }

  @Override
  public List<String> getChangedAttributes() {
    return StreamSupport.stream(changedAttributesProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  @Override
  public String getAction() {
    return bufferAsString(actionProp.getValue());
  }

  @Override
  public String getExternalFormReference() {
    return bufferAsString(externalFormReferenceProp.getValue());
  }

  @Override
  public Map<String, String> getCustomHeaders() {
    return MsgPackConverter.convertToStringMap(customHeadersProp.getValue());
  }

  @Override
  public long getCreationTimestamp() {
    return creationTimestampProp.getValue();
  }

  @Override
  public String getElementId() {
    return bufferAsString(elementIdProp.getValue());
  }

  @Override
  public long getElementInstanceKey() {
    return elementInstanceKeyProp.getValue();
  }

  @Override
  public String getBpmnProcessId() {
    return bufferAsString(bpmnProcessIdProp.getValue());
  }

  @Override
  public int getProcessDefinitionVersion() {
    return processDefinitionVersionProp.getValue();
  }

  @Override
  public long getProcessDefinitionKey() {
    return processDefinitionKeyProp.getValue();
  }

  public UserTaskRecord setProcessDefinitionKey(final long processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public UserTaskRecord setProcessDefinitionVersion(final int version) {
    processDefinitionVersionProp.setValue(version);
    return this;
  }

  public UserTaskRecord setBpmnProcessId(final String bpmnProcessId) {
    bpmnProcessIdProp.setValue(bpmnProcessId);
    return this;
  }

  public UserTaskRecord setBpmnProcessId(final DirectBuffer bpmnProcessId) {
    bpmnProcessIdProp.setValue(bpmnProcessId);
    return this;
  }

  public UserTaskRecord setElementInstanceKey(final long elementInstanceKey) {
    elementInstanceKeyProp.setValue(elementInstanceKey);
    return this;
  }

  public UserTaskRecord setElementId(final String elementId) {
    elementIdProp.setValue(elementId);
    return this;
  }

  public UserTaskRecord setElementId(final DirectBuffer elementId) {
    elementIdProp.setValue(elementId);
    return this;
  }

  public UserTaskRecord setCreationTimestamp(final long creationTimestamp) {
    creationTimestampProp.setValue(creationTimestamp);
    return this;
  }

  public UserTaskRecord setCustomHeaders(final DirectBuffer buffer) {
    customHeadersProp.setValue(buffer, 0, buffer.capacity());
    return this;
  }

  public UserTaskRecord setExternalFormReference(final DirectBuffer externalFormReference) {
    externalFormReferenceProp.setValue(externalFormReference);
    return this;
  }

  public UserTaskRecord setExternalFormReference(final String externalFormReference) {
    externalFormReferenceProp.setValue(externalFormReference);
    return this;
  }

  public UserTaskRecord setAction(final String action) {
    actionProp.setValue(action);
    return this;
  }

  public UserTaskRecord setAction(final DirectBuffer action) {
    actionProp.setValue(action);
    return this;
  }

  public UserTaskRecord setChangedAttributes(final List<String> changedAttributes) {
    changedAttributesProp.reset();
    changedAttributes.forEach(
        attribute -> changedAttributesProp.add().wrap(BufferUtil.wrapString(attribute)));
    return this;
  }

  public UserTaskRecord setFormKey(final long formKey) {
    formKeyProp.setValue(formKey);
    return this;
  }

  public UserTaskRecord setFollowUpDate(final String followUpDate) {
    followUpDateProp.setValue(followUpDate);
    return this;
  }

  public UserTaskRecord setFollowUpDate(final DirectBuffer followUpDate) {
    followUpDateProp.setValue(followUpDate);
    return this;
  }

  public UserTaskRecord setDueDate(final String dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public UserTaskRecord setDueDate(final DirectBuffer dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public UserTaskRecord setCandidateUsersList(final List<String> candidateUsers) {
    candidateUsersListProp.reset();
    candidateUsers.forEach(
        tenantId -> candidateUsersListProp.add().wrap(BufferUtil.wrapString(tenantId)));
    return this;
  }

  public UserTaskRecord setCandidateGroupsList(final List<String> candidateGroups) {
    candidateGroupsListProp.reset();
    candidateGroups.forEach(
        tenantId -> candidateGroupsListProp.add().wrap(BufferUtil.wrapString(tenantId)));
    return this;
  }

  public UserTaskRecord setAssignee(final String assignee) {
    assigneeProp.setValue(assignee);
    return this;
  }

  public UserTaskRecord setAssignee(final DirectBuffer assignee) {
    assigneeProp.setValue(assignee);
    return this;
  }

  public UserTaskRecord setUserTaskKey(final long userTaskKey) {
    userTaskKeyProp.setValue(userTaskKey);
    return this;
  }

  public UserTaskRecord setCandidateGroupsChanged() {
    changedAttributesProp.add().wrap(CANDIDATE_GROUPS_VALUE);
    return this;
  }

  public UserTaskRecord setCandidateUsersChanged() {
    changedAttributesProp.add().wrap(CANDIDATE_USERS_VALUE);
    return this;
  }

  public UserTaskRecord setDueDateChanged() {
    changedAttributesProp.add().wrap(DUE_DATE_VALUE);
    return this;
  }

  public UserTaskRecord setFollowUpDateChanged() {
    changedAttributesProp.add().wrap(FOLLOW_UP_DATE_VALUE);
    return this;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public UserTaskRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return MsgPackConverter.convertToMap(variableProp.getValue());
  }

  public UserTaskRecord setVariables(final DirectBuffer variables) {
    variableProp.setValue(variables);
    return this;
  }

  @JsonIgnore
  public DirectBuffer getCustomHeadersBuffer() {
    return customHeadersProp.getValue();
  }

  @JsonIgnore
  public ArrayProperty<StringValue> getChangedAttributesProp() {
    return changedAttributesProp;
  }

  public UserTaskRecord setChangedAttributesProp(
      final ArrayProperty<StringValue> changedAttributes) {
    changedAttributesProp.reset();
    changedAttributes.forEach(attribute -> changedAttributesProp.add().wrap(attribute));
    return this;
  }

  @JsonIgnore
  public DirectBuffer getAssigneeBuffer() {
    return assigneeProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getDueDateBuffer() {
    return dueDateProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getFollowUpDateBuffer() {
    return followUpDateProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variableProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getBpmnProcessIdBuffer() {
    return bpmnProcessIdProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getElementIdBuffer() {
    return elementIdProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getExternalFormReferenceBuffer() {
    return externalFormReferenceProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  @JsonIgnore
  public DirectBuffer getActionBuffer() {
    return actionProp.getValue();
  }

  @JsonIgnore
  public String getActionOrDefault(final String defaultAction) {
    final String action = bufferAsString(actionProp.getValue());
    return action.isEmpty() ? defaultAction : action;
  }

  @Override
  public long getProcessInstanceKey() {
    return processInstanceKeyProp.getValue();
  }

  public UserTaskRecord setProcessInstanceKey(final long key) {
    processInstanceKeyProp.setValue(key);
    return this;
  }
}
