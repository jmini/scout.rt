/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.ext.browser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ResourceManager;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.rap.rwt.service.ServiceHandler;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.internal.events.EventTypes;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * <h3>BrowserSupport</h3> adding hyperlink callback support as in normal swt to the rwt browser
 * <p>
 * Adding support for registering/unregistering (publishing) local resources.
 * 
 * @author imo
 * @since 3.8.0
 */
public class BrowserExtension {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BrowserExtension.class);
  private static final Pattern LOCAL_URL_PATTERN = Pattern.compile("(['\"])(http://local[?/][^'\"]*)(['\"])", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private final Browser m_browser;
  private final HashMap<String, String> m_hyperlinkMap;
  private final String m_serviceHandlerId;
  private ServiceHandler m_serviceHandler;
  private ServerPushSession m_pushSession;
  //
  private HashSet<String> m_tempFileNames = new HashSet<String>();

  public BrowserExtension(Browser b) {
    m_browser = b;
    m_hyperlinkMap = new HashMap<String, String>();
    m_serviceHandlerId = UUID.randomUUID().toString();
  }

  /**
   * @return the unique {@link UUID} serviceHandlerId
   */
  public String getServiceHandlerId() {
    return m_serviceHandlerId;
  }

  // TODO RAP 2.0 migration - old code
//  private String getUiCallbackId() {
//    return getClass().getName() + "" + hashCode();
//  }

  public void attach() {
    if (m_serviceHandler == null) {
      // TODO RAP 2.0 migration - old code
// old code       UICallBack.activate(getUiCallbackId());
      m_pushSession = new ServerPushSession();
      m_pushSession.start();
      m_serviceHandler = new ServiceHandler() {
        @Override
        public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
          String localUrl = m_hyperlinkMap.get(request.getParameter("p"));
          if (localUrl == null) {
            return;
          }
          fireLocationChangedEvent(localUrl);
        }
      };
      RWT.getServiceManager().registerServiceHandler(m_serviceHandlerId, m_serviceHandler);
    }
  }

  public void detach() {
    // TODO RAP 2.0 migration - old code
// old code   UICallBack.deactivate(getUiCallbackId());
    m_pushSession.stop();
    clearLocalHyperlinkCache();
    clearResourceCache();
    if (m_serviceHandler != null) {
      m_serviceHandler = null;
      RWT.getServiceManager().unregisterServiceHandler(m_serviceHandlerId);
    }
  }

  /**
   * Adds a text resource that is encoded with the given <code>charset</code>.
   * <p>
   * By specifying an <code>option</code> other than <code>NONE</code> the resource will be versioned and/or compressed.
   * As compressing is only intended for resources that contain JavaScript, versioning might be useful for other
   * resources as well. When versioning is enabled a version number is appended to the resources' name which is derived
   * from its content.
   * </p>
   * <p>
   * 
   * @param content
   *          the content of the resource to add.
   * @return the web url of the resource valid for calls from outside
   */
  public String addResource(String name, InputStream content) {
    name = name.replaceAll("\\\\", "/");
    if (name == null || name.length() == 0) {
      return null;
    }
    if (!name.startsWith("/")) {
      name = "/" + name;
    }
    String uniqueName = m_serviceHandlerId + name;
    m_tempFileNames.add(uniqueName);
    ResourceManager resourceManager = RWT.getResourceManager();
    resourceManager.register(uniqueName, content);
    return resourceManager.getLocation(uniqueName);
  }

  public void clearResourceCache() {
    ResourceManager resourceManager = RWT.getResourceManager();
    try {
      for (String name : m_tempFileNames) {
        resourceManager.unregister(name);
      }
    }
    finally {
      m_tempFileNames.clear();
    }
  }

  /**
   * @param html
   *          replaces all http://local/... urls by a ajax callback with a {@link LocationEvent} in html text
   * @param childDepth
   *          when the document is inside a iframe or thelike, then childDepth is 1, if it is in addition inside an
   *          embed tag (such as svg), then childDepth is 2.
   */
  public String adaptLocalHyperlinks(String html, int childDepth) {
    String p;
    if (childDepth <= 0) {
      p = "this";
    }
    else {
      p = "parent";
      for (int i = 1; i < childDepth; i++) {
        p = "parent." + p;
      }
    }
    return rewriteLocalHyperlinks(html, p, m_serviceHandlerId, m_hyperlinkMap);
  }

  public void clearLocalHyperlinkCache() {
    m_hyperlinkMap.clear();
  }

  private void fireLocationChangedEvent(final String location) {

    m_browser.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          Constructor<?> c = LocationEvent.class.getDeclaredConstructor(Object.class);
          c.setAccessible(true);
          //send changing
          LocationEvent event = (LocationEvent) c.newInstance(new Event());
          event.location = location;
          event.top = true;

          //send changing
          final Listener[] locationChangingListeners = m_browser.getListeners(EventTypes.LOCALTION_CHANGING);
          if (locationChangingListeners != null) {
            for (Listener l : locationChangingListeners) {
              if (l instanceof LocationListener) {
                ((LocationListener) l).changing(event);
              }
            }
          }
          //send changed
          final Listener[] locationChangedListeners = m_browser.getListeners(EventTypes.LOCALTION_CHANGED);
          if (locationChangedListeners != null) {
            for (Listener l : locationChangedListeners) {
              if (l instanceof LocationListener) {
                ((LocationListener) l).changed(event);
              }
            }
          }

        }
        catch (Throwable t) {
          //nop
        }
      }
    });
  }

  /**
   * Replace all href="http://local/... references in the html file and replace by an ajax call.
   * 
   * @param html
   * @param rwtServiceHandler
   *          is called with the parameter "p" containing the local url key to the generatedMapping
   * @param generatedMappings
   *          is being filled up with the generated mappings
   * @return the rewritten html
   */
  private static String rewriteLocalHyperlinks(String html, String ajaxParentContext, String rwtServiceHandler, Map<String /*externalKey*/, String /*url*/> generatedMappings) {
    if (html == null) {
      return html;
    }
    StringBuilder buf = new StringBuilder();
    Matcher m = LOCAL_URL_PATTERN.matcher(html);
    int nextFind = 0;
    while (m.find(nextFind)) {
      String localUrl = m.group(2);
      String externalKey = "" + generatedMappings.size();
      StringBuilder urlBuf = new StringBuilder();
      urlBuf.append("?");
      urlBuf.append("nocache='+new Date().getTime()+'");
      urlBuf.append("&amp;");
      urlBuf.append("custom_service_handler");
      urlBuf.append("=");
      urlBuf.append(rwtServiceHandler);
      urlBuf.append("&amp;");
      urlBuf.append("p");
      urlBuf.append("=");
      urlBuf.append(externalKey);
      String encodedURL = RWT.getResponse().encodeURL(urlBuf.toString());
      String callableURL = "javascript:a=" + ajaxParentContext + ".qx.net.HttpRequest.create();a.open('GET','" + encodedURL + "',true);a.send(null);";
      buf.append(html.substring(nextFind, m.start()));
      buf.append(m.group(1));
      buf.append(callableURL);
      buf.append(m.group(3));
      //register
      generatedMappings.put(externalKey, localUrl);
      //next
      nextFind = m.end();
    }
    if (nextFind == 0) {
      return html;
    }
    if (nextFind < html.length()) {
      buf.append(html.substring(nextFind));
    }
    return buf.toString();
  }

}
