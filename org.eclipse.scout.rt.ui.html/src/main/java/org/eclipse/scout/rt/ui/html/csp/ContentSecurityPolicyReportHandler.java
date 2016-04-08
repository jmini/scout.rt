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
package org.eclipse.scout.rt.ui.html.csp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.rt.platform.util.CompareUtility;
import org.eclipse.scout.rt.platform.util.IOUtility;
import org.eclipse.scout.rt.server.commons.servlet.HttpServletControl;
import org.eclipse.scout.rt.ui.html.AbstractUiServletRequestHandler;
import org.eclipse.scout.rt.ui.html.UiServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler contributes to the {@link UiServlet} as the POST handler for <code>/csp.cgi</code>
 * <p>
 * It is used to collect Content-Security-Policy violations.
 * <p>
 * see {@link HttpServletControl}
 *
 * @since 5.s
 */
public class ContentSecurityPolicyReportHandler extends AbstractUiServletRequestHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ContentSecurityPolicyReportHandler.class);

  @Override
  public boolean handlePost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    //serve only /csp
    if (!CompareUtility.equals("/csp.cgi", req.getPathInfo())) {
      return false;
    }
    final String jsonData = IOUtility.getContent(req.getReader());
    LOG.warn("CSP-REPORT: {}", jsonData);
    return true;
  }
}