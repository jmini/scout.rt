package org.eclipse.scout.rt.ui.html.json.table;

import org.eclipse.scout.rt.client.ui.basic.table.columnfilter.ITableColumnFilter;
import org.eclipse.scout.rt.ui.html.json.IJsonObject;
import org.eclipse.scout.rt.ui.html.json.JsonObjectUtility;
import org.json.JSONObject;

public class JsonTableColumnFilter implements IJsonObject {
  private final JsonTable m_jsonTable;
  private final ITableColumnFilter<?> m_filter;

  public JsonTableColumnFilter(ITableColumnFilter<?> filter, JsonTable table) {
    m_filter = filter;
    m_jsonTable = table;
  }

  public ITableColumnFilter<?> getFilter() {
    return m_filter;
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = JsonObjectUtility.newOrderedJSONObject();
    json.put("column", m_jsonTable.getColumnId(m_filter.getColumn()));
    json.put("selectedValues", m_filter.getSelectedValues());
    return json;
  }
}
