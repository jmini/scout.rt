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
package org.eclipse.scout.rt.client.mobile.ui.basic.table.columns;

import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;

/**
 * @since 3..9.0
 */
public interface IRowSummaryColumn extends IColumn<String> {

  String PROP_DEFAULT_DRILL_DOWN_STYLE = "defaultDrillDownStyle";

  String DRILL_DOWN_STYLE_NONE = "none";
  String DRILL_DOWN_STYLE_ICON = "icon";
  String DRILL_DOWN_STYLE_BUTTON = "button";

  String getDefaultDrillDownStyle();

  void setDefaultDrillDownStyle(String drillDownStyle);

}
