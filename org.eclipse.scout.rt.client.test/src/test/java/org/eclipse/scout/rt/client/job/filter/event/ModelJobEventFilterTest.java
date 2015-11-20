/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.job.filter.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.job.IJobManager;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.job.listener.JobEvent;
import org.eclipse.scout.rt.platform.job.listener.JobEventType;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class ModelJobEventFilterTest {

  @Test
  public void test() {
    IClientSession session1 = mock(IClientSession.class);
    IClientSession session2 = mock(IClientSession.class);

    IFilter<JobEvent> filter = ModelJobEventFilter.INSTANCE;

    // not a model job (no future)
    JobEvent event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(null);
    assertFalse(filter.accept(event));

    // not a model job (no ClientRunContext)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()));
    assertFalse(filter.accept(event));

    // not a model job (no ClientRunContext)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(RunContexts.empty())));
    assertFalse(filter.accept(event));

    // not a model job (no mutex and not session on ClientRunContext)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(ClientRunContexts.empty())));
    assertFalse(filter.accept(event));

    // not a model job (no mutex)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(ClientRunContexts.empty().withSession(session1, false))));
    assertFalse(filter.accept(event));

    // not a model job (wrong mutex type)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(ClientRunContexts.empty().withSession(session1, false))
            .withMutex(new Object())));
    assertFalse(filter.accept(event));

    // not a model job (different session on ClientRunContext and mutex)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(ClientRunContexts.empty()
                .withSession(session1, false))
            .withMutex(session2)));
    assertFalse(filter.accept(event));

    // not a model job (no session on ClientRunContext)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(ClientRunContexts.empty()
                .withSession(null, false))
            .withMutex(session1)));
    assertFalse(filter.accept(event));

    // this is a model job (same session on ClientRunContext and mutex)
    event = new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN)
        .withFuture(Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
            .withRunContext(ClientRunContexts.empty()
                .withSession(session1, false))
            .withMutex(session1)));
    assertTrue(filter.accept(event));
  }
}