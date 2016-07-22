package org.eclipse.scout.rt.server;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.eclipse.scout.rt.shared.session.ISessionListener;
import org.eclipse.scout.rt.shared.session.SessionEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

/**
 * Tests for listeners in {@link AbstractServerSession}
 */
public class ServerSessionListenerTest {

  private IServerSession m_testSession;
  private ISessionListener m_listenerMock;

  @Before
  public void setup() {
    m_testSession = new TestServerSession();
    m_listenerMock = mock(ISessionListener.class);
    m_testSession.addListener(m_listenerMock);
  }

  @After
  public void cleanup() {
    m_testSession.removeListener(m_listenerMock);
  }

  @Test
  public void testListenerStartSession() {
    m_testSession.start("");
    verify(m_listenerMock, times(1)).sessionChanged(argThat(hasSessionType(SessionEvent.TYPE_STARTED)));
    verifyNoMoreInteractions(m_listenerMock);
  }

  @Test
  public void testListenerStopSession() {
    m_testSession.start("");
    m_testSession.stop();
    verify(m_listenerMock, times(1)).sessionChanged(argThat(hasSessionType(SessionEvent.TYPE_STARTED)));
    verify(m_listenerMock, times(1)).sessionChanged(argThat(hasSessionType(SessionEvent.TYPE_STOPPING)));
    verify(m_listenerMock, times(1)).sessionChanged(argThat(hasSessionType(SessionEvent.TYPE_STOPPED)));

    verifyNoMoreInteractions(m_listenerMock);
  }

  private ArgumentMatcher<SessionEvent> hasSessionType(final int sessionType) {
    return new ArgumentMatcher<SessionEvent>() {

      @Override
      public boolean matches(Object argument) {
        return argument instanceof SessionEvent
            && ((SessionEvent) argument).getType() == sessionType;
      }
    };
  }
}