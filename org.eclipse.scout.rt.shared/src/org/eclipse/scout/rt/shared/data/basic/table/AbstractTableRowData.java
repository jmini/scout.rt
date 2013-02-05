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
package org.eclipse.scout.rt.shared.data.basic.table;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scout.commons.holders.ITableHolder;

/**
 * Bean that stores the contents of a scout table row. This class is intended to be extended for every table type and
 * the column values are expected to be added as Java bean properties.
 * 
 * @since 3.8.2
 */
public abstract class AbstractTableRowData implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * same value as {@link ITableHolder#STATUS_NON_CHANGED}.
   */
  public static final int STATUS_NON_CHANGED = ITableHolder.STATUS_NON_CHANGED;

  /**
   * same value as {@link ITableHolder#STATUS_INSERTED}.
   */
  public static final int STATUS_INSERTED = ITableHolder.STATUS_INSERTED;

  /**
   * same value as {@link ITableHolder#STATUS_UPDATED}.
   */
  public static final int STATUS_UPDATED = ITableHolder.STATUS_UPDATED;

  /**
   * same value as {@link ITableHolder#STATUS_DELETED}.
   */
  public static final int STATUS_DELETED = ITableHolder.STATUS_DELETED;

  private int m_rowState;
  private Map<String, Object> m_customColumnValues;

  /**
   * @return Returns this row's state.
   * @see #STATUS_NON_CHANGED
   * @see #STATUS_INSERTED
   * @see #STATUS_UPDATED
   * @see #STATUS_DELETED
   */
  public int getRowState() {
    return m_rowState;
  }

  /**
   * Sets this row's state
   * 
   * @param rowState
   * @see #STATUS_NON_CHANGED
   * @see #STATUS_INSERTED
   * @see #STATUS_UPDATED
   * @see #STATUS_DELETED
   */
  public void setRowState(int rowState) {
    m_rowState = rowState;
  }

  /**
   * @return Returns a map with custom column values or <code>null</code>, if none have been set.
   */
  public Map<String, Object> getCustomColumnValues() {
    return m_customColumnValues;
  }

  /**
   * Sets a map with custom column values.
   * 
   * @param customColumnValues
   */
  public void setCustomColumnValues(Map<String, Object> customColumnValues) {
    m_customColumnValues = customColumnValues;
  }

  /**
   * Returns the custom column value with the given <code>columnId</code> or <code>null</code> if it does not exist.
   * 
   * @param columnId
   * @return
   */
  public Object getCustomColumnValue(String columnId) {
    if (m_customColumnValues == null) {
      return null;
    }
    return m_customColumnValues.get(columnId);
  }

  /**
   * Sets a custom column value for the given <code>columnId</code>. If <code>value</code> is <code>null</code>, the
   * custom column entry is removed from the map.
   * 
   * @param columnId
   * @param value
   */
  public void setCustomColumnValue(String columnId, Object value) {
    if (value == null) {
      if (m_customColumnValues != null) {
        m_customColumnValues.remove(columnId);
      }
      return;
    }

    if (m_customColumnValues == null) {
      m_customColumnValues = new HashMap<String, Object>();
    }
    m_customColumnValues.put(columnId, value);
  }
}
