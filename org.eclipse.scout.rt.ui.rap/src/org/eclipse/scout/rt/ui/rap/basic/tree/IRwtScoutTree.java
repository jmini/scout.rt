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
package org.eclipse.scout.rt.ui.rap.basic.tree;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.ui.rap.basic.IRwtScoutComposite;
import org.eclipse.swt.widgets.Tree;

public interface IRwtScoutTree extends IRwtScoutComposite<ITree> {

  void setEnabledFromScout(boolean enabled);

  @Override
  Tree getUiField();

  TreeViewer getUiTreeViewer();
}
