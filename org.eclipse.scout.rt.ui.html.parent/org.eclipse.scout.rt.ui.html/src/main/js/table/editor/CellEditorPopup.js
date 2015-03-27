scout.CellEditorPopup = function(column, row, cell) {
  scout.CellEditorPopup.parent.call(this);
  this.table = column.table;
  this.column = column;
  this.row = row;
  this.cell = cell;
  this.keyStrokeAdapter = new scout.CellEditorPopupKeyStrokeAdapter(this);
};
scout.inherits(scout.CellEditorPopup, scout.Popup);

scout.CellEditorPopup.prototype.render = function($parent) {
  scout.CellEditorPopup.parent.prototype.render.call(this, $parent);
  var offsetBounds,
    field = this.cell.field,
    $cell = this.table.$cell(this.column, this.row.$row);

  field.render(this.$container);
  offsetBounds = scout.graphics.offsetBounds($cell);
  this.setLocation(new scout.Point(offsetBounds.x, offsetBounds.y));
  scout.graphics.setSize(this.$container, offsetBounds.width, offsetBounds.height);
  scout.graphics.setSize(field.$container, offsetBounds.width, offsetBounds.height);
  scout.HtmlComponent.get(field.$container).layout();

  scout.keyStrokeManager.installAdapter(this.$container, this.keyStrokeAdapter);
  setTimeout(function() {
    this.$container.installFocusContext('auto', this.table.session.jsonSessionId);
  }.bind(this), 0);
};

scout.CellEditorPopup.prototype.remove = function() {
  scout.CellEditorPopup.parent.prototype.remove.call(this);
  scout.keyStrokeManager.uninstallAdapter(this.keyStrokeAdapter);
  if (this._mouseDownHandler) {
    $(document).off('mousedown', this._mouseDownHandler);
    this._mouseDownHandler = null;
  }
};

scout.CellEditorPopup.prototype._attachCloseHandler = function() {
  //FIXME CGU merge with popup.js
  this._mouseDownHandler = this._onMouseDown.bind(this);
  $(document).on('mousedown', this._mouseDownHandler);

  if (this.$origin) {
    scout.scrollbars.attachScrollHandlers(this.$origin, this.remove.bind(this));
  }
};

scout.CellEditorPopup.prototype._onMouseDown = function(event) {
  var $target = $(event.target);
  //FIXME CGU only necessary if popup would open with mousedown?
//  if ($target.is(this.$container)) {
//    return;
//  }

  // close the popup only if the click happened outside of the popup
  if (this.$container.has($target).length === 0) {
    this._onMouseDownOutside();
  }
};

scout.CellEditorPopup.prototype._onMouseDownOutside = function(event) {
  this.completeEdit();
};

scout.CellEditorPopup.prototype.completeEdit = function() {
  var field = this.cell.field;

  // There is no blur event when the popup gets closed -> trigger blur so that the field may react (accept display text, close popups etc.)
  var $activeElement = $(document.activeElement);
  $activeElement.blur();

  this.table.sendCompleteCellEdit(field.id);
  //FIXME CGU what if there is a validation error?
  this.cell.field.destroy();
  this.remove();
};

scout.CellEditorPopup.prototype.cancelEdit = function() {
  this.table.sendCancelCellEdit(this.cell.field.id);
  this.cell.field.destroy();
  this.remove();
};
