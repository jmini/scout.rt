/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonRequest {

  /**
   * The {@link JsonRequest} which is currently associated with the current thread.
   */
  public static final ThreadLocal<JsonRequest> CURRENT = new ThreadLocal<>();

  public static final String PROP_STARTUP = "startup";
  public static final String PROP_UNLOAD = "unload";
  public static final String PROP_LOG = "log";
  public static final String PROP_POLL_FOR_BACKGROUND_JOBS = "pollForBackgroundJobs";
  public static final String PROP_CANCEL = "cancel";
  public static final String PROP_UI_SESSION_ID = "uiSessionId";
  public static final String PROP_EVENTS = "events";

  private final JSONObject m_request;

  /**
   * Creates a new JsonRequest instance.
   *
   * @throws IllegalArgumentException
   *           when mandatory property uiSessionId is not set
   */
  public JsonRequest(JSONObject request) {
    if (!request.has(PROP_UI_SESSION_ID)) {
      throw new IllegalArgumentException("Missing property '" + PROP_UI_SESSION_ID + "' in request " + request);
    }
    m_request = request;
  }

  protected JSONObject getRequestObject() {
    return m_request;
  }

  public String getUiSessionId() {
    return m_request.getString(PROP_UI_SESSION_ID);
  }

  public List<JsonEvent> getEvents() {
    JSONArray events = m_request.optJSONArray(PROP_EVENTS);
    if (events == null) {
      return new ArrayList<>(0);
    }
    List<JsonEvent> actionList = new ArrayList<>(events.length());
    for (int i = 0; i < events.length(); i++) {
      JSONObject json = events.getJSONObject(i);
      actionList.add(JsonEvent.fromJson(json));
    }
    return actionList;
  }

  public boolean isStartupRequest() {
    return m_request.optBoolean(PROP_STARTUP);
  }

  public boolean isUnloadRequest() {
    return m_request.optBoolean(PROP_UNLOAD);
  }

  public boolean isPollForBackgroundJobsRequest() {
    return m_request.has(PROP_POLL_FOR_BACKGROUND_JOBS);
  }

  public boolean isCancelRequest() {
    return m_request.has(PROP_CANCEL);
  }

  public boolean isLogRequest() {
    return m_request.has(PROP_LOG);
  }

  @Override
  public String toString() {
    return JsonObjectUtility.toString(m_request);
  }
}