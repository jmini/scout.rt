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
package org.eclipse.scout.rt.platform.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.job.internal.JobManager;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class ScheduleDelayedTest {

  private IJobManager m_jobManager;

  @Before
  public void before() {
    m_jobManager = new JobManager();
  }

  @After
  public void after() {
    m_jobManager.shutdown();
  }

  @Test
  public void testScheduleDelayed() throws ProcessingException {
    final List<Long> protocol = Collections.synchronizedList(new ArrayList<Long>());

    long delayNano = TimeUnit.SECONDS.toNanos(1);
    long tStartNano = System.nanoTime();
    long assertToleranceNano = TimeUnit.MILLISECONDS.toNanos(200);

    IFuture<Void> future = m_jobManager.schedule(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        protocol.add(System.nanoTime());
        return null;
      }
    }, delayNano, TimeUnit.NANOSECONDS, Jobs.newInput(RunContexts.empty()));

    // verify
    assertTrue(m_jobManager.awaitDone(Jobs.newFutureFilter().andMatchFutures(future), 30, TimeUnit.SECONDS));
    assertEquals(1, protocol.size());
    Long actualExecutionTime = protocol.get(0);
    long expectedExecutionTime = tStartNano + delayNano;
    long expectedExecutionTimeMin = expectedExecutionTime;
    long expectedExecutionTimeMax = expectedExecutionTime + assertToleranceNano;

    if (actualExecutionTime < expectedExecutionTimeMin || actualExecutionTime > expectedExecutionTimeMax) {
      fail(String.format("actualExecutionTime=%s, expectedExecutionTime=[%s;%s]", actualExecutionTime, expectedExecutionTimeMin, expectedExecutionTimeMax));
    }
  }
}
