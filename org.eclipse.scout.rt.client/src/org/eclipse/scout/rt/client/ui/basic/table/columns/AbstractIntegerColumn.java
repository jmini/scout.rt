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
package org.eclipse.scout.rt.client.ui.basic.table.columns;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.integerfield.AbstractIntegerField;

/**
 * Column holding Integer
 */
public abstract class AbstractIntegerColumn extends AbstractColumn<Integer> implements IIntegerColumn {
  // DO NOT init members, this has the same effect as if they were set AFTER
  // initConfig()
  private String m_format;
  private boolean m_groupingUsed;
  private NumberFormat m_fmt;

  public AbstractIntegerColumn() {
    super();
  }

  @Override
  protected int getConfiguredHorizontalAlignment() {
    return 1;
  }

  /*
   * Configuration
   */

  /**
   * Configures the format used to render the value. See {@link DecimalFormat#applyPattern(String)} for more information
   * about the expected format.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Format of this column.
   */
  @ConfigProperty(ConfigProperty.STRING)
  @Order(140)
  @ConfigPropertyValue("null")
  protected String getConfiguredFormat() {
    return null;
  }

  /**
   * Configures whether grouping is used for this column. If grouping is used, the values may be displayed with a digit
   * group separator.
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   * 
   * @return {@code true} if grouping is used for this column, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(150)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredGroupingUsed() {
    return true;
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    setFormat(getConfiguredFormat());
    setGroupingUsed(getConfiguredGroupingUsed());
  }

  /*
   * Runtime
   */
  @Override
  public void setFormat(String s) {
    m_format = s;
    m_fmt = null;
  }

  @Override
  public String getFormat() {
    return m_format;
  }

  @Override
  public void setGroupingUsed(boolean b) {
    m_groupingUsed = b;
    m_fmt = null;
  }

  @Override
  public boolean isGroupingUsed() {
    return m_groupingUsed;
  }

  @Override
  protected Integer parseValueInternal(ITableRow row, Object rawValue) throws ProcessingException {
    Integer validValue = null;
    if (rawValue == null) {
      validValue = null;
    }
    else if (rawValue instanceof Integer) {
      validValue = (Integer) rawValue;
    }
    else if (rawValue instanceof Number) {
      validValue = ((Number) rawValue).intValue();
    }
    else {
      throw new ProcessingException("invalid Integer value in column '" + getClass().getSimpleName() + "': " + rawValue + " class=" + rawValue.getClass());
    }
    return validValue;
  }

  @Override
  protected IFormField prepareEditInternal(ITableRow row) throws ProcessingException {
    AbstractIntegerField f = new AbstractIntegerField() {
    };
    f.setFormat(getFormat());
    f.setGroupingUsed(isGroupingUsed());
    return f;
  }

  @Override
  protected void decorateCellInternal(Cell cell, ITableRow row) {
    super.decorateCellInternal(cell, row);
    if (cell.getValue() != null) {
      cell.setText(getNumberFormat().format(((Integer) cell.getValue()).intValue()));
    }
    else {
      cell.setText("");
    }
  }

  @Override
  public NumberFormat getNumberFormat() {
    if (m_fmt == null) {
      if (getFormat() != null) {
        DecimalFormat x = (DecimalFormat) DecimalFormat.getNumberInstance(LocaleThreadLocal.get());
        x.applyPattern(getFormat());
        x.setMinimumFractionDigits(0);
        x.setMaximumFractionDigits(0);
        m_fmt = x;
      }
      else {
        m_fmt = NumberFormat.getNumberInstance(LocaleThreadLocal.get());
        m_fmt.setMinimumFractionDigits(0);
        m_fmt.setMaximumFractionDigits(0);
        m_fmt.setGroupingUsed(isGroupingUsed());
      }
    }
    return m_fmt;
  }

}
