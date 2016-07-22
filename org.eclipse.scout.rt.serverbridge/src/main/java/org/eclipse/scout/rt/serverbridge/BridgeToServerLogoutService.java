package org.eclipse.scout.rt.serverbridge;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Order;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.clientnotification.IClientNodeId;
import org.eclipse.scout.rt.server.session.ServerSessionProvider;
import org.eclipse.scout.rt.shared.clientnotification.IClientNotificationService;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.rt.shared.services.common.security.ILogoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Order(4900)
public class BridgeToServerLogoutService implements ILogoutService {
  private static final Logger LOG = LoggerFactory.getLogger(BridgeToServerLogoutService.class);

  @Override
  public void logout() {
    try {
      BEANS.get(IAccessControlService.class).clearCacheOfCurrentUser();

      // Manually stop session, because we don't have a HTTP session in "bridge" mode (see org.eclipse.scout.rt.server.ServiceTunnelServlet.ScoutSessionBindingListener)
      IServerSession session = ServerSessionProvider.currentSession();
      session.stop();
      BEANS.get(IClientNotificationService.class).unregisterSession(IClientNodeId.CURRENT.get(), session.getId(), session.getUserId());
    }
    catch (Exception e) {
      LOG.warn("Failed to stop session.", e);
    }
  }
}