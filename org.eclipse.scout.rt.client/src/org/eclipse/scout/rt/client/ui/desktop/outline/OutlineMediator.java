/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.desktop.outline;

import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithNodes;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

/**
 * @since 3.8.0
 */
public class OutlineMediator {

  public void mediateTreeNodesChanged(IPageWithNodes pageWithNodes) {
    try {
      pageWithNodes.rebuildTableInternal();
    }
    catch (ProcessingException e1) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(e1);
    }
  }

  public void mediateTreeNodeDropAction(TreeEvent e, IPageWithTable<? extends ITable> pageWithTable) {
    ITableRow row = pageWithTable.getTableRowFor(e.getNode());
    ITable table = pageWithTable.getTable();
    if (row != null) {
      table.getUIFacade().fireRowDropActionFromUI(row, e.getDropObject());
    }
  }

  public void mediateTreeNodesDragRequest(TreeEvent e, IPageWithTable<? extends ITable> pageWithTable) {
    ITableRow[] rows = pageWithTable.getTableRowsFor(e.getNodes());
    ITable table = pageWithTable.getTable();
    table.getUIFacade().setSelectedRowsFromUI(rows);
    TransferObject t = table.getUIFacade().fireRowsDragRequestFromUI();
    if (t != null) {
      e.setDragObject(t);
    }
  }

  public void mediateTreeNodeAction(TreeEvent e, IPageWithTable<? extends ITable> pageWithTable) {
    if (e.isConsumed()) {
      return;
    }

    ITableRow row = pageWithTable.getTableRowFor(e.getNode());
    ITable table = pageWithTable.getTable();
    if (row != null) {
      e.consume();
      /*
       * ticket 78684: this line added
       */
      table.getUIFacade().setSelectedRowsFromUI(new ITableRow[]{row});
      table.getUIFacade().fireRowActionFromUI(row);
    }
  }

  public void fetchTableRowMenus(TreeEvent e, IPageWithTable<? extends ITable> pageWithTable) {
    if (!pageWithTable.isShowTableRowMenus()) {
      return;
    }

    ITableRow row = pageWithTable.getTableRowFor(e.getNode());
    ITable table = pageWithTable.getTable();
    if (row != null) {
      table.getUIFacade().setSelectedRowsFromUI(new ITableRow[]{row});
      IMenu[] menus = table.getUIFacade().fireRowPopupFromUI();
      if (menus != null) {
        e.addPopupMenus(menus);
      }
    }
  }

  public void fetchTableEmptySpaceMenus(TreeEvent e, IPageWithTable<? extends ITable> pageWithTable) {
    ITable table = pageWithTable.getTable();
    if (!pageWithTable.isShowEmptySpaceMenus()) {
      return;
    }

    IMenu[] menus = table.getUIFacade().fireEmptySpacePopupFromUI();
    if (menus != null) {
      e.addPopupMenus(menus);
    }
  }

  public void mediateTableRowFilterChanged(IPage page) {
    if (page == null || page.getTree() == null || page.isLeaf()) {
      return;
    }

    page.getTree().applyNodeFilters();
  }

  public void mediateTableRowOrderChanged(TableEvent e, IPageWithTable pageWithTable) {
    if (pageWithTable == null || pageWithTable.getTree() == null || pageWithTable.isLeaf()) {
      return;
    }

    IPage[] childNodes = pageWithTable.getUpdatedChildPagesFor(e.getRows());
    if (pageWithTable.getTree() != null) {
      pageWithTable.getTree().updateChildNodeOrder(pageWithTable, childNodes);
    }
  }

  public void mediateTableRowsUpdated(TableEvent e, IPageWithTable pageWithTable) {
    if (pageWithTable == null || pageWithTable.getTree() == null || pageWithTable.isLeaf()) {
      return;
    }

    IPage[] childNodes = pageWithTable.getUpdatedChildPagesFor(e.getRows());
    if (pageWithTable.getTree() != null) {
      pageWithTable.getTree().updateChildNodes(pageWithTable, childNodes);
    }
  }

  public void mediateTableRowsInserted(ITableRow[] tableRows, IPage[] childPages, IPageWithTable pageWithTable) {
    if (pageWithTable == null || pageWithTable.getTree() == null || pageWithTable.isLeaf()) {
      return;
    }

    pageWithTable.getTree().addChildNodes(pageWithTable, childPages);
  }

  public void mediateTableRowsDeleted(IPage[] childNodes, IPageWithTable pageWithTable) {
    if (pageWithTable == null || pageWithTable.getTree() == null || pageWithTable.isLeaf()) {
      return;
    }

    pageWithTable.getTree().removeChildNodes(pageWithTable, childNodes);
  }

  public void mediateTableRowAction(TableEvent e, IPage page) {
    if (e.isConsumed()) {
      return;
    }

    ITreeNode node = null;
    if (page instanceof IPageWithNodes) {
      node = ((IPageWithNodes) page).getTreeNodeFor(e.getFirstRow());
    }
    else if (page instanceof IPageWithTable<?>) {
      node = ((IPageWithTable<?>) page).getTreeNodeFor(e.getFirstRow());
    }

    if (node != null) {
      e.consume();
      if (page.getTree() != null) {
        page.getTree().getUIFacade().setNodeSelectedAndExpandedFromUI(node);
      }
    }
  }

  public void mediateTableRowDropAction(TableEvent e, IPageWithNodes pageWithNodes) {
    if (pageWithNodes == null || pageWithNodes.getTree() == null) {
      return;
    }

    ITreeNode node = pageWithNodes.getTreeNodeFor(e.getFirstRow());
    if (node != null) {
      pageWithNodes.getTree().getUIFacade().fireNodeDropActionFromUI(node, e.getDropObject());
    }
  }

  public void mediateTableRowPopup(TableEvent e, IPageWithNodes pageWithNodes) {
    ITreeNode node = pageWithNodes.getTreeNodeFor(e.getFirstRow());
    if (node instanceof IPageWithTable<?>) {
      IPageWithTable<?> tablePage = (IPageWithTable<?>) node;
      IMenu[] menus = tablePage.getTable().fetchMenusForRowsInternal(new ITableRow[0]);
      if (menus != null) {
        e.addPopupMenus(menus);
      }
    }
    else if (node instanceof IPageWithNodes) {
      IMenu[] menus = pageWithNodes.getTree().fetchMenusForNodesInternal(new ITreeNode[]{node});
      if (menus != null) {
        e.addPopupMenus(menus);
      }
    }
  }
}
