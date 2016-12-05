/*******************************************************************************
 * Copyright (c) 2010-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.internal;

import java.awt.GraphicsEnvironment;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.IBeanDecorationFactory;
import org.eclipse.scout.rt.platform.IBeanManager;
import org.eclipse.scout.rt.platform.IPlatform;
import org.eclipse.scout.rt.platform.IPlatformListener;
import org.eclipse.scout.rt.platform.PlatformEvent;
import org.eclipse.scout.rt.platform.PlatformStateLatch;
import org.eclipse.scout.rt.platform.SimpleBeanDecorationFactory;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.config.ConfigUtility;
import org.eclipse.scout.rt.platform.config.IConfigurationValidator;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.PlatformDevModeProperty;
import org.eclipse.scout.rt.platform.exception.PlatformException;
import org.eclipse.scout.rt.platform.inventory.ClassInventory;
import org.eclipse.scout.rt.platform.util.TypeCastUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 5.1
 */
public class PlatformImplementor implements IPlatform {

  private static final Logger LOG = LoggerFactory.getLogger(PlatformImplementor.class);

  private static final String SCOUT_HEADLESS_PROPERTY = "scout.headless";
  private static final String AWT_HEADLESS_PROPERTY = "java.awt.headless";

  private final ReentrantReadWriteLock m_platformLock = new ReentrantReadWriteLock(true);
  private final Object m_platformStartedLock = new Object();
  private final AtomicReference<State> m_state; // may be read at any time by any thread
  private BeanManagerImplementor m_beanManager;

  public PlatformImplementor() {
    m_state = new AtomicReference<>(State.PlatformStopped);
  }

  @Override
  public State getState() {
    return m_state.get();
  }

  @Override
  public IBeanManager getBeanManager() {
    // use lock to ensure the caller waits until the platform has been started completely
    m_platformLock.readLock().lock();
    try {
      if (getState() == State.PlatformInvalid) {
        throw new PlatformException("The platform is in an invalid state.");
      }
      return m_beanManager;
    }
    finally {
      m_platformLock.readLock().unlock();
    }
  }

  @Override
  public void awaitPlatformStarted() {
    if (isPlatformStarted()) {
      return;
    }
    synchronized (m_platformStartedLock) {
      while (!isPlatformStarted()) {
        try {
          m_platformStartedLock.wait();
        }
        catch (InterruptedException e) {
          // nop
        }
      }
    }
  }

  protected boolean isPlatformStarted() {
    final State state = getState();
    if (state == null || state == State.PlatformInvalid) {
      throw new PlatformException("The platform is in an invalid state.");
    }
    if (state == State.PlatformStopping) {
      throw new PlatformException("The platform is stopping.");
    }
    return state == State.PlatformStarted;
  }

  protected void notifyPlatformStarted() {
    synchronized (m_platformStartedLock) {
      m_platformStartedLock.notifyAll();
    }
  }

  @Override
  public void start() {
    start(null);
  }

  @Override
  @SuppressWarnings("squid:S1181")
  public void start(PlatformStateLatch stateLatch) {
    try {
      m_platformLock.writeLock().lock();
      try {
        if (stateLatch != null) {
          stateLatch.release();
        }
        if (m_state.get() != State.PlatformStopped) {
          throw new PlatformException("Platform is not stopped [m_state=" + m_state.get() + "]");
        }

        try {
          validateHeadless();
          m_beanManager = createBeanManager();
          //now all IPlatformListener are registered and can receive platform events
          changeState(State.BeanManagerPrepared, true);

          validateConfiguration();
          initBeanDecorationFactory();

          changeState(State.BeanManagerValid, true);
          startCreateImmediatelyBeans();
        }
        catch (RuntimeException | Error e) {
          LOG.error("Error during platform startup", e);
          changeState(State.PlatformInvalid, true);
          throw e;
        }
      }
      finally {
        //since we are using a reentrant lock, platform beans can be accessed within platform listeners
        //lock has to be released after the State.BeanManagerValid change to make sure everything is initialized correctly, before beans can be accessed.
        m_platformLock.writeLock().unlock();
      }
      changeState(State.PlatformStarted, true);
    }
    finally {
      notifyPlatformStarted();
    }
  }

  protected void validateHeadless() {
    final boolean scoutHeadless = ConfigUtility.getPropertyBoolean(SCOUT_HEADLESS_PROPERTY, true);
    String awtHeadlessStr = System.getProperty(AWT_HEADLESS_PROPERTY);
    final boolean awtHeadless = TypeCastUtility.castValue(awtHeadlessStr, boolean.class);
    String autoSetMsg = "";
    if (scoutHeadless && !awtHeadless) {
      System.setProperty(AWT_HEADLESS_PROPERTY, "true");
      autoSetMsg = " (automatically set by Scout)";
      awtHeadlessStr = "true";
    }
    boolean graphicsEnvironmentHeadless = GraphicsEnvironment.isHeadless();
    LOG.info("Headless mode: {}={}, {}={}{}, GraphicsEnvironment.isHeadless()={}", SCOUT_HEADLESS_PROPERTY, scoutHeadless,
        AWT_HEADLESS_PROPERTY, awtHeadlessStr, autoSetMsg, graphicsEnvironmentHeadless);

    if (scoutHeadless && !graphicsEnvironmentHeadless) {
      LOG.warn("{} is 'true', but GraphicsEnvironment.isHeadless() reports 'false'. AWT seems to have been already initialized. "
          + "Please try setting the system property {}=true manually when starting the VM. You can turn off this message by setting {}=false",
          SCOUT_HEADLESS_PROPERTY, AWT_HEADLESS_PROPERTY, SCOUT_HEADLESS_PROPERTY);
    }
  }

  protected void validateConfiguration() {
    if (!ConfigUtility.isInitialized()) {
      LOG.info("No {} found. Running with empty configuration.", ConfigUtility.CONFIG_FILE_NAME);
    }
    int errorCount = 0;
    List<IConfigurationValidator> validators = BEANS.all(IConfigurationValidator.class);
    for (Entry<String, String> config : ConfigUtility.getAllEntries().entrySet()) {
      if (!isConfigEntryValid(validators, config)) {
        errorCount++;
        LOG.error("Config property with key '{}' does not exist or has an invalid value.", config.getKey());
      }
    }
    // the validators are not used anymore. Allow to free resources
    for (IConfigurationValidator validator : validators) {
      validator.dispose();
    }
    if (errorCount > 0) {
      throw new PlatformException("Cannot start platform due to " + errorCount + " invalid config properties");
    }
  }

  protected boolean isConfigEntryValid(List<IConfigurationValidator> validators, Entry<String, String> config) {
    for (IConfigurationValidator validator : validators) {
      if (validator.isValid(config.getKey(), config.getValue())) {
        return true;
      }
    }
    return false;
  }

  protected BeanManagerImplementor createBeanManager() {
    BeanManagerImplementor beanManager = new BeanManagerImplementor();
    Set<Class> allBeans = new BeanFilter().collect(ClassInventory.get());
    for (Class<?> bean : allBeans) {
      beanManager.registerClass(bean);
    }
    return beanManager;
  }

  protected void initBeanDecorationFactory() {
    if (m_beanManager.getBeanDecorationFactory() != null) {
      return;
    }
    IBean<IBeanDecorationFactory> bean = m_beanManager.optBean(IBeanDecorationFactory.class);
    if (bean != null) {
      m_beanManager.setBeanDecorationFactory(bean.getInstance());
      return;
    }
    LOG.warn("Using {}. Please verify that this application really has no client or server side {}", SimpleBeanDecorationFactory.class.getName(), IBeanDecorationFactory.class.getSimpleName());
    m_beanManager.setBeanDecorationFactory(new SimpleBeanDecorationFactory());
  }

  protected void startCreateImmediatelyBeans() {
    ((BeanManagerImplementor) m_beanManager).startCreateImmediatelyBeans();
  }

  @Override
  public void stop() {
    changeState(State.PlatformStopping, true);

    m_platformLock.writeLock().lock();
    try {
      changeState(State.PlatformStopped, false);
      destroyBeanManager();
    }
    finally {
      m_platformLock.writeLock().unlock();
    }
  }

  protected void destroyBeanManager() {
    m_beanManager = null;
  }

  protected void changeState(State newState, boolean throwOnIllegalStateChange) {
    if (newState == null) {
      throw new IllegalArgumentException("new state cannot be null.");
    }
    if (m_state.get() == newState) {
      return;
    }

    EnumSet<State> possibleExpectedCurrentStates = getPreviousStates(newState);
    if (possibleExpectedCurrentStates == null || possibleExpectedCurrentStates.size() == 0) {
      throw new IllegalStateException("Unknown state transition: '" + newState + "' has no preceeding state defined.");
    }

    boolean changed = false;
    for (State expectedCurrentState : possibleExpectedCurrentStates) {
      changed = m_state.compareAndSet(expectedCurrentState, newState);
      if (changed) {
        break;
      }
    }
    if (!changed && throwOnIllegalStateChange) {
      throw new PlatformException(
          "Invalid state change. Current state (" + m_state.get() + ") cannot be changed to " + newState + ". A state change to " + newState + " is only allowed in these states " + possibleExpectedCurrentStates);
    }
    fireStateEvent(newState);
  }

  protected static EnumSet<State> getPreviousStates(State reference) {
    switch (reference) {
      case BeanManagerPrepared:
        return EnumSet.of(State.PlatformStopped);
      case BeanManagerValid:
        return EnumSet.of(State.BeanManagerPrepared);
      case PlatformStarted:
        return EnumSet.of(State.BeanManagerValid);
      case PlatformStopping:
        return EnumSet.of(State.PlatformStarted);
      case PlatformStopped:
        return EnumSet.of(State.PlatformStopping);
      case PlatformInvalid:
        return EnumSet.of(State.BeanManagerPrepared, State.BeanManagerValid, State.PlatformStarted, State.PlatformStopping, State.PlatformStopped);
    }
    return EnumSet.noneOf(State.class);
  }

  @SuppressWarnings("squid:S1181")
  protected void fireStateEvent(State newState) {
    PlatformEvent event = new PlatformEvent(this, newState);
    try {
      for (IBean<IPlatformListener> bean : m_beanManager.getBeans(IPlatformListener.class)) {
        IPlatformListener listener = bean.getInstance();
        listener.stateChanged(event);
      }
    }
    catch (RuntimeException | Error e) {
      LOG.error("Error during event listener notification.", e);
      changeState(State.PlatformInvalid, true);
      throw e;
    }
  }

  @Override
  public boolean inDevelopmentMode() {
    return CONFIG.getPropertyValue(PlatformDevModeProperty.class); // cannot be null
  }
}
