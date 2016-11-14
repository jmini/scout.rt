package org.eclipse.scout.rt.serverbridge;

import org.eclipse.scout.rt.client.servicetunnel.http.MultiSessionCookieStoreInstallListener;
import org.eclipse.scout.rt.platform.PlatformEvent;

public class BridgeToServerMultiSessionCookieStoreInstallListener extends MultiSessionCookieStoreInstallListener {

  @Override
  public void stateChanged(PlatformEvent event) {
    // NOP - Don't install MultiSessionCookieStore in bridge mode!
  }
}