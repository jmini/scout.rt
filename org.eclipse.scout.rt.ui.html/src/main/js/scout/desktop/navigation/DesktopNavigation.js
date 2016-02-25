/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.DesktopNavigation = function() {
  scout.DesktopNavigation.parent.call(this);
  this.$container;
  this.$body;
  this.viewButtons;
  this.toolBarVisible = false;
};
scout.inherits(scout.DesktopNavigation, scout.Widget);

scout.DesktopNavigation.DEFAULT_STYLE_WIDTH; // Configured in sizes.css
scout.DesktopNavigation.BREADCRUMB_STYLE_WIDTH; // Configured in sizes.css
scout.DesktopNavigation.MIN_SPLITTER_SIZE = 49; // not 50px because last pixel is the border (would not look good)

scout.DesktopNavigation.prototype._init = function(model) {
  scout.DesktopNavigation.parent.prototype._init.call(this, model);
  this.desktop = this.parent;
  scout.DesktopNavigation.DEFAULT_STYLE_WIDTH = $.pxToNumber(scout.styles.get('desktop-navigation', 'width').width);
  scout.DesktopNavigation.BREADCRUMB_STYLE_WIDTH = $.pxToNumber(scout.styles.get('desktop-navigation-breadcrumb', 'width').width);
};

scout.DesktopNavigation.prototype._render = function($parent) {
  this.$container = $parent.appendDiv('desktop-navigation');
  this.htmlComp = new scout.HtmlComponent(this.$container, this.session);
  this.htmlComp.setLayout(new scout.DesktopNavigationLayout(this));

  this.viewButtons = scout.create('ViewButtons', {
    parent: this
  });
  this.viewButtons.render(this.$container);

  this.$body = this.$container.appendDiv('navigation-body')
    .on('mousedown', this._onNavigationMousedown.bind(this));
  this.htmlCompBody = new scout.HtmlComponent(this.$body, this.session);
  this.htmlCompBody.setLayout(new scout.SingleLayout());

  this._renderToolBarVisible();
};

scout.DesktopNavigation.prototype._onNavigationMousedown = function(event) {
  if (this.outline.inBackground) {
    this.desktop.bringOutlineToFront(this.outline);
  }
};

scout.DesktopNavigation.prototype.onOutlineChanged = function(outline, bringToFront) {
  if (this.outline === outline) {
    return;
  }
  if (this.outline) {
    this.outline.remove();

    // Make sure new outline uses same display style as old
    if (outline.autoToggleBreadcrumbStyle) {
      var displayStyle = this.outline.displayStyle;
      outline.setDisplayStyle(displayStyle);
    }
  }

  this.outline = outline;
  this.outline.setBreadcrumbTogglingThreshold(scout.DesktopNavigation.BREADCRUMB_STYLE_WIDTH);
  this.outline.render(this.$body);
  this.outline.invalidateLayoutTree();
  this.outline.handleOutlineContent(bringToFront);
  this.viewButtons.onOutlineChanged(outline);
  this.outline.validateFocus();
  // Layout immediate to prevent flickering when breadcrumb mode is enabled
  // but not initially while desktop gets rendered because it will be done at the end anyway
  if (this.desktop.rendered) {
    this.outline.validateLayoutTree();
  }
};

scout.DesktopNavigation.prototype.sendToBack = function() {
  this.viewButtons.viewMenuTab.sendToBack();
  this.outline.sendToBack();
};

scout.DesktopNavigation.prototype.bringToFront = function() {
  this.viewButtons.viewMenuTab.bringToFront();
  this.outline.bringToFront();
};

scout.DesktopNavigation.prototype.setToolBarVisible = function(toolBarVisible) {
  this.toolBarVisible = toolBarVisible;
  if (this.rendered) {
    this._renderToolBarVisible();
  }
};

scout.DesktopNavigation.prototype._renderToolBarVisible = function() {
  if (this.toolBarVisible) {
    this._renderToolBar();
  } else {
    this._removeToolBar();
  }
};

scout.DesktopNavigation.prototype._renderToolBar = function() {
  if (this._$toolBar) {
    return;
  }
  this._$toolBar = this.$body.beforeDiv('header-tools');
  this.desktop.actions.forEach(function(action) {
    action._customCssClasses = "header-tool-item compact";
    action.popupOpeningDirectionX = 'left';
    action.textVisible = false;
    action.subMenuIconVisible = false;
    action.render(this._$toolBar);
  }.bind(this));

  if (this.desktop.actions) {
    this.desktop.actions[this.desktop.actions.length - 1].$container.addClass('last');
  }
};

scout.DesktopNavigation.prototype._removeToolBar = function() {
  if (!this._$toolBar) {
    return;
  }
  this._$toolBar.remove();
  this._$toolBar = null;
};