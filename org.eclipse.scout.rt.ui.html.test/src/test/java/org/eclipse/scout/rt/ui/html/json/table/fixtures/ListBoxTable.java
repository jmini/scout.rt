package org.eclipse.scout.rt.ui.html.json.table.fixtures;

import org.eclipse.scout.rt.client.ui.basic.table.AbstractTableRowBuilder;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableRow;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBox;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBox.DefaultListBoxTable.ActiveColumn;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBox.DefaultListBoxTable.KeyColumn;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBox.DefaultListBoxTable.TextColumn;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;

/**
 * <h3>{@link ListBoxTable}</h3>
 *
 * @author nbu
 */
public class ListBoxTable extends AbstractListBox<Long> {

  @Override
  public AbstractTableRowBuilder<Long> getTableRowBuilder() {
    return new P_TableRowBuilder();
  }

  @SuppressWarnings("unchecked")
  protected AbstractListBox<Long>.DefaultListBoxTable.KeyColumn getKeyColumnInternal() {
    return getTable().getColumnSet().getColumnByClass(KeyColumn.class);
  }

  protected AbstractListBox<?>.DefaultListBoxTable.TextColumn getTextColumnInternal() {
    return getTable().getColumnSet().getColumnByClass(TextColumn.class);
  }

  protected AbstractListBox<?>.DefaultListBoxTable.ActiveColumn getActiveColumnInternal() {
    return getTable().getColumnSet().getColumnByClass(ActiveColumn.class);
  }

  private class P_TableRowBuilder extends AbstractTableRowBuilder<Long> {

    @Override
    public ITableRow createTableRow(ILookupRow<Long> dataRow) {
      TableRow tableRow = (TableRow) super.createTableRow(dataRow);

      // fill values to tableRow
      getKeyColumnInternal().setValue(tableRow, dataRow.getKey());
      getTextColumnInternal().setValue(tableRow, dataRow.getText());
      getActiveColumnInternal().setValue(tableRow, dataRow.isActive());

      // enable/disabled row
      tableRow.setEnabled(dataRow.isEnabled());

      // hint for inactive
      if (!dataRow.isActive()) {
        getTextColumnInternal().setValue(tableRow, dataRow.getText() + " (" + TEXTS.get("InactiveState") + ")");
      }
      return tableRow;
    }

    @Override
    protected ITableRow createEmptyTableRow() {
      return new TableRow(getTable().getColumnSet());
    }
  }

}