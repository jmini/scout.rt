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
package org.eclipse.scout.rt.ui.rap.window.filedownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.service.ServiceHandler;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.swt.widgets.Display;

public class RwtScoutDownloadHandler implements ServiceHandler {
  private static IScoutLogger LOG = ScoutLogManager.getLogger(RwtScoutDownloadHandler.class);

  private File m_file;
  private URI m_bundleURI;
  private final String m_fileName;
  private final String m_contentType;
  private final String m_requestId;
  private RwtScoutDownloadDialog m_sdd;

  private RwtScoutDownloadHandler(String id, String contentType, String fileName) {
    m_requestId = id;
    m_fileName = fileName;
    m_contentType = contentType;
  }

  public RwtScoutDownloadHandler(String id, File file, String contentType, String fileName) {
    this(id, contentType, fileName);
    m_file = file;
  }

  public RwtScoutDownloadHandler(String id, URI bundleURI, String contentType, String fileName) {
    this(id, contentType, fileName);
    m_bundleURI = bundleURI;
  }

  public void startDownload() {
    RWT.getServiceManager().unregisterServiceHandler(m_requestId);
    if (m_sdd != null) {
      m_sdd.close();
      m_sdd = null;
    }

    RWT.getServiceManager().registerServiceHandler(m_requestId, this);
    m_sdd = new RwtScoutDownloadDialog(null, getURL());
    m_sdd.open();
  }

  protected String getURL() {
    String url = RWT.getServiceManager().getServiceHandlerUrl(m_requestId);
    String encodedURL = RWT.getResponse().encodeURL(url);
    return encodedURL;
  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    try {
      writeResponse(response);
    }
    finally {
      if (m_sdd != null) {
        Display display = m_sdd.getShell().getDisplay();
        display.asyncExec(new Runnable() {

          @Override
          public void run() {
            m_sdd.close();
            m_sdd = null;
          }
        });
      }
      RWT.getServiceManager().unregisterServiceHandler(m_requestId);
    }
  }

  protected void writeResponse(HttpServletResponse response) throws IOException {
    if (StringUtility.hasText(m_contentType)) {
      response.setContentType(m_contentType);
    }
    else {
      response.setContentType("application/octet-stream");
    }
    String contentDisposition = "attachment; filename=\"" + m_fileName + "\"";
    response.setHeader("Content-Disposition", contentDisposition);

    ServletOutputStream outStream = response.getOutputStream();
    InputStream srcStream = null;

    if (m_file != null) {
      srcStream = new FileInputStream(m_file);
      if (m_file.length() < Integer.MAX_VALUE) {
        response.setContentLength((int) m_file.length());
      }
    }
    else if (m_bundleURI != null) {
      srcStream = m_bundleURI.toURL().openStream();
    }

    if (srcStream != null) {
      try {
        byte[] content = new byte[1024];
        int bytesRead = 0;
        while ((bytesRead = srcStream.read(content)) > 0) {
          outStream.write(content, 0, bytesRead);
        }
      }
      catch (IOException e) {
        LOG.error("IOEception while writing file.", e);
      }
      finally {
        srcStream.close();
      }
    }
  }

}
