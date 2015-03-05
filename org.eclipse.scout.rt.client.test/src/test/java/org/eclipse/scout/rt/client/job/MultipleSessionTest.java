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
package org.eclipse.scout.rt.client.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.filter.AlwaysFilter;
import org.eclipse.scout.commons.job.IFuture;
import org.eclipse.scout.commons.job.IRunnable;
import org.eclipse.scout.commons.job.JobExecutionException;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.job.filter.ClientSessionFilter;
import org.eclipse.scout.rt.client.job.internal.ModelJobManager;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.testing.commons.BlockingCountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MultipleSessionTest {

  private IModelJobManager m_jobManager;

  private IClientSession m_clientSession1;
  private IClientSession m_clientSession2;

  @Before
  public void before() {
    m_jobManager = new ModelJobManager();
    m_clientSession1 = mock(IClientSession.class);
    m_clientSession2 = mock(IClientSession.class);
  }

  @After
  public void after() {
    m_jobManager.shutdown();
    ISession.CURRENT.remove();
  }

  @Test
  public void testMutalExclusion() throws JobExecutionException, InterruptedException {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch latch1 = new BlockingCountDownLatch(2);
    final BlockingCountDownLatch latch2 = new BlockingCountDownLatch(2);

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S1");
        latch1.countDownAndBlock();
      }
    }, ClientJobInput.empty().name("job-1-S1").session(m_clientSession1));

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S1");
        latch2.countDownAndBlock();
      }
    }, ClientJobInput.empty().name("job-2-S1").session(m_clientSession1));

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S2");
        latch1.countDownAndBlock();
      }
    }, ClientJobInput.empty().name("job-1-S2").session(m_clientSession2));

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S2");
        latch2.countDownAndBlock();
      }
    }, ClientJobInput.empty().name("job-2-S2").session(m_clientSession2));

    assertTrue(latch1.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S2"), protocol);
    latch1.unblock();

    assertTrue(latch2.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S2", "job2-S1", "job2-S2"), protocol);
    latch2.unblock();

    assertTrue(m_jobManager.waitUntilDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));
  }

  @Test
  public void testCancel() throws JobExecutionException, InterruptedException {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch latch1 = new BlockingCountDownLatch(2);
    final BlockingCountDownLatch latch2 = new BlockingCountDownLatch(1);
    final BlockingCountDownLatch interruptedLatch = new BlockingCountDownLatch(1);

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S1");
        try {
          latch1.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job1-S1-interrupted");
        }
        finally {
          interruptedLatch.countDown();
        }
      }
    }, ClientJobInput.empty().name("job-1-S1").session(m_clientSession1));

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S1");
        try {
          latch2.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job2-S1-interrupted");
        }
      }
    }, ClientJobInput.empty().name("job-2-S1").session(m_clientSession1));

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job1-S2");
        try {
          latch1.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job1-S2-interrupted");
        }
      }
    }, ClientJobInput.empty().name("job-1-S2").session(m_clientSession2));

    m_jobManager.schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        protocol.add("job2-S2");
        try {
          latch2.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("job2-S2-interrupted");
        }
      }
    }, ClientJobInput.empty().name("job-2-S2").session(m_clientSession2));

    assertTrue(latch1.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S2"), protocol);

    m_jobManager.cancel(new ClientSessionFilter(m_clientSession1), true); // cancel all jobs of session1

    assertTrue(interruptedLatch.await());

    latch1.unblock();

    assertTrue(latch2.await());
    assertEquals(CollectionUtility.hashSet("job1-S1", "job1-S1-interrupted", "job1-S2", "job2-S2"), protocol);
    latch2.unblock();

    assertTrue(m_jobManager.waitUntilDone(new AlwaysFilter<IFuture<?>>(), 30, TimeUnit.SECONDS));
  }
}
