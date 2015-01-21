// SCOUT GUI
// (c) Copyright 2013-2014, BSI Business Systems Integration AG

scout.GroupBox = function() {
  scout.GroupBox.parent.call(this);
  this.formFields = [];
  this._addAdapterProperties('formFields');
  this.$body;
  this.$bodyContainer; // $scrollable if groupbox is scrollable, otherwise same as _$body;
  this._$groupBoxTitle;

  this.controls = [];
  this.systemButtons = [];
  this.customButtons = [];
  this.processButtons = [];
};
scout.inherits(scout.GroupBox, scout.CompositeField);

scout.GroupBox.prototype._renderProperties = function() {
  scout.GroupBox.parent.prototype._renderProperties.call(this);

  this._renderBorderVisible(this.borderVisible);
};

scout.GroupBox.prototype._render = function($parent) {
  var env = scout.HtmlEnvironment,
    htmlComp = this.addContainer($parent, this.mainBox ? 'root-group-box' : 'group-box', new scout.GroupBoxLayout(this)),
    htmlBodyContainer;

  if (this.mainBox) {
    htmlComp.layoutData = null;
  }

  this.$label = $('<span>').html(this.label);
  this._$groupBoxTitle = this.$container
    .appendDiv('group-box-title')
    .append(this.$label);

  this.$body = this.$container.appendDiv('group-box-body');
  this.$bodyContainer = this.$body;

  htmlBodyContainer = new scout.HtmlComponent(this.$bodyContainer, this.session);
  if (this.scrollable) {
    this.$bodyContainer = scout.scrollbars.install(this.$body, {
      createHtmlComponent: true
    });
    htmlBodyContainer = scout.HtmlComponent.get(this.$bodyContainer);
    this.session.detachHelper.pushScrollable(this.$bodyContainer);
  }
  htmlBodyContainer.setLayout(new scout.LogicalGridLayout(env.formColumnGap, env.formRowGap));

  this._createFieldArraysByType();
  for (var i = 0; i < this.controls.length; i++) {
    this.controls[i].render(this.$bodyContainer);
  }
};

scout.GroupBox.prototype._remove = function() {
  if (this.scrollable) {
    this.session.detachHelper.removeScrollable(this.$bodyContainer);
  }
};

scout.GroupBox.prototype._createFieldArraysByType = function() {
  this.controls = [];
  this.systemButtons = [];
  this.customButtons = [];
  this.processButtons = [];

  var i, field;
  for (i = 0; i < this.formFields.length; i++) {
    field = this.formFields[i];
    if (field instanceof scout.Button) {
      if (field.processButton) {
        this.processButtons.push(field);
        if (field.systemType !== scout.Button.SYSTEM_TYPE.NONE) {
          this.systemButtons.push(field);
        }
        else {
          this.customButtons.push(field);
        }
      } else {
        this.controls.push(field);
      }
    } else {
      this.controls.push(field);
    }
  }
};

/**
 * @override CompositeField.js
 */
scout.GroupBox.prototype.getFields = function() {
  return this.controls;
};

scout.GroupBox.prototype._renderBorderVisible = function(borderVisible) {
  if (borderVisible && this.borderDecoration === 'auto') {
    borderVisible = this._computeBorderVisible();
  }

  if (!borderVisible) {
    this.$body.addClass('y-padding-invisible');
  }
};

//Don't include in renderProperties, it is not necessary to execute it initially because renderBorderVisible is executed already
scout.GroupBox.prototype._renderBorderDecoration = function() {
  this._renderBorderVisible(this.borderVisible);
};

/**
 *
 * @returns false if it is the mainbox. Or if the groupbox contains exactly one tablefield which has an invisible label
 */
scout.GroupBox.prototype._computeBorderVisible = function() {
  var fields = this.getFields();
  if (this.mainBox) {
    return false;
  } else if (fields.length === 1 && fields[0].objectType === 'TableField' && !fields[0].labelVisible) {
    fields[0].$container.addClass('single');
    return false;
  }
  return true;
};

/**
 * @override FormField.js
 */
scout.GroupBox.prototype._renderLabelVisible = function(visible) {
  // TODO AWE: (concept) discuss with C.GU -> auf dem GUI server korrigieren oder im Browser UI?
  // --> kein hack für main-box, wenn die auf dem model ein label hat, hat es im UI auch eins
  this._$groupBoxTitle.setVisible(visible && this.label && !this.mainBox);
};
