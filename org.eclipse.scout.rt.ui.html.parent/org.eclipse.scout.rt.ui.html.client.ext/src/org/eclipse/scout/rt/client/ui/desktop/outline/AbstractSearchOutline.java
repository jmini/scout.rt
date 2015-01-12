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
package org.eclipse.scout.rt.client.ui.desktop.outline;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.tree.AbstractTree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeUIFacade;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

public class AbstractSearchOutline extends AbstractOutline5 implements ISearchOutline {

  @Override
  protected String getConfiguredTitle() {
    return TEXTS.get("Search");
  }

  @Override
  protected String getConfiguredIconId() {
    return "\\f002"; //Icons.Search
  }

  @Override
  public void search() throws ProcessingException {
    execSearch(getSearchQuery());
  }

  protected void execSearch(String query) throws ProcessingException {

  }

  @Override
  public void setSearchQuery(String searchQuery) {
    propertySupport.setPropertyString(PROP_SEARCH_QUERY, searchQuery);
  }

  @Override
  public String getSearchQuery() {
    return propertySupport.getPropertyString(PROP_SEARCH_QUERY);
  }

  @Override
  public void setSearchStatus(String searchStatus) {
    propertySupport.setPropertyString(PROP_SEARCH_STATUS, searchStatus);
  }

  @Override
  public String getSearchStatus() {
    return propertySupport.getPropertyString(PROP_SEARCH_STATUS);
  }

  @Override
  protected ITreeUIFacade createUIFacade() {
    return new P_UIFacade();
  }

  @Override
  public ISearchOutlineUiFacade getUIFacade() {
    return (ISearchOutlineUiFacade) super.getUIFacade();
  }

  protected class P_UIFacade extends AbstractTree.P_UIFacade implements ISearchOutlineUiFacade {

    @Override
    public void search(String query) {
      try {
        pushUIProcessor();
        setProperty(PROP_SEARCH_QUERY, query);
        AbstractSearchOutline.this.search();
      }
      catch (ProcessingException se) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(se);
      }
      finally {
        popUIProcessor();
      }
    }

  }
}
