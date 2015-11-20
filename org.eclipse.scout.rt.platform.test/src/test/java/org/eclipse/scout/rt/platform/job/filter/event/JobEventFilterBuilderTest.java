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
package org.eclipse.scout.rt.platform.job.filter.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.IJobManager;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.job.listener.JobEvent;
import org.eclipse.scout.rt.platform.job.listener.JobEventType;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class JobEventFilterBuilderTest {

  @Test
  public void test() {
    Object mutex1 = new Object();
    Object mutex2 = new Object();

    IFuture<?> future1 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("A"));

    IFuture<?> future2 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("B")
        .withRunContext(RunContexts.empty())
        .withMutex(mutex1));

    IFuture<?> future3 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("C")
        .withRunContext(new P_RunContext())
        .withMutex(mutex1));

    IFuture<?> future4 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("D")
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS));

    IFuture<?> future5 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("E")
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS));

    IFuture<?> future6 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("E")
        .withRunContext(new P_RunContext())
        .withPeriodicExecutionAtFixedRate(1, TimeUnit.SECONDS));

    IFuture<?> future7 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("F")
        .withMutex(mutex1));

    IFuture<?> future8 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("G")
        .withMutex(mutex1));

    IFuture<?> future9 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("H")
        .withMutex(mutex2));

    IFuture<?> future10 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput()
        .withName("I")
        .withRunContext(new P_RunContext())
        .withMutex(mutex1));

    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future1)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future2)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future3)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future4)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future5)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future6)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future7)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future8)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future9)));
    assertTrue(new JobEventFilterBuilder().toFilter().accept(newAboutToRunJobEvent(future10)));

    // with filtering for futures but event is not Future related (e.g. Shutdown event)
    IFilter<JobEvent> filter = new JobEventFilterBuilder()
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .toFilter();
    assertFalse(filter.accept(newShutdownJobEvent()));

    // with filtering for futures but wrong event type
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.SCHEDULED)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .toFilter();

    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future2)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future10)));

    // with filtering for futures
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .toFilter();

    assertTrue(filter.accept(newAboutToRunJobEvent(future1)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future8)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // additionally with filtering for single executing jobs
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .toFilter();
    assertTrue(filter.accept(newAboutToRunJobEvent(future1)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future8)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // additionally with filtering for mutex
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchMutex(mutex1)
        .toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // additionally with filtering for jobs running on behalf of a RunContext
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchMutex(mutex1)
        .andMatchRunContext(RunContext.class)
        .toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // additionally with filtering for jobs running on behalf of a specific P_RunContext
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchMutex(mutex1)
        .andMatchRunContext(P_RunContext.class)
        .toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // additionally with filtering for names
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchMutex(mutex1)
        .andMatchRunContext(P_RunContext.class)
        .andMatchName("A", "B", "C")
        .toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future10)));

    // additionally with filtering for other names
    filter = new JobEventFilterBuilder()
        .andMatchEventType(JobEventType.ABOUT_TO_RUN, JobEventType.DONE)
        .andMatchFuture(future1, future2, future3, future4, future8, future9, future10)
        .andAreSingleExecuting()
        .andMatchMutex(mutex1)
        .andMatchRunContext(P_RunContext.class)
        .andMatchName("D", "E", "F")
        .toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future2)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future6)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future10)));
  }

  @Test
  public void testFutureExclusion() {
    Object mutex = new Object();

    IFuture<?> future1 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput());
    IFuture<?> future2 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput());
    IFuture<?> future3 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput());
    IFuture<?> future4 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput());
    IFuture<?> future5 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput());
    IFuture<?> future6 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput().withMutex(mutex));
    IFuture<?> future7 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput().withMutex(mutex));
    IFuture<?> future8 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput().withMutex(mutex));
    IFuture<?> future9 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput().withMutex(mutex));
    IFuture<?> future10 = Jobs.schedule(mock(IRunnable.class), Jobs.newInput().withMutex(mutex));

    // One future exclusion with not other criteria
    IFilter<JobEvent> filter = Jobs.newEventFilterBuilder()
        .andMatchNotFuture(future8).toFilter();
    assertTrue(filter.accept(newAboutToRunJobEvent(future1)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future4)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future5)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future6)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // Multiple future exclusions with not other criteria
    filter = Jobs.newEventFilterBuilder()
        .andMatchNotFuture(future8, future9).toFilter();
    assertTrue(filter.accept(newAboutToRunJobEvent(future1)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future2)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future3)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future4)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future5)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future6)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // One future exclusion with other criterion (mutex)
    filter = Jobs.newEventFilterBuilder()
        .andMatchMutex(mutex)
        .andMatchNotFuture(future8).toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future2)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future6)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));

    // Multiple future exclusion with other criterion (mutex)
    filter = Jobs.newEventFilterBuilder()
        .andMatchMutex(mutex)
        .andMatchNotFuture(future8, future9).toFilter();
    assertFalse(filter.accept(newAboutToRunJobEvent(future1)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future2)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future3)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future4)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future5)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future6)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future7)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future8)));
    assertFalse(filter.accept(newAboutToRunJobEvent(future9)));
    assertTrue(filter.accept(newAboutToRunJobEvent(future10)));
  }

  private static JobEvent newAboutToRunJobEvent(IFuture<?> future) {
    return new JobEvent(mock(IJobManager.class), JobEventType.ABOUT_TO_RUN).withFuture(future);
  }

  private static JobEvent newShutdownJobEvent() {
    return new JobEvent(mock(IJobManager.class), JobEventType.SHUTDOWN);
  }

  private static class P_RunContext extends RunContext {

    @Override
    public RunContext copy() {
      final P_RunContext copy = new P_RunContext();
      copy.copyValues(this);
      return copy;
    }
  }
}