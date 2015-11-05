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
scout.MenuItemsOrder = function(session, objectType) {
  this.session = session;
  this.objectType = objectType;
  this.emptySpaceTypes = ['EmptySpace'];
  this.selectionTypes = ['SingleSelection', 'MultiSelection'];
};

scout.MenuItemsOrder.prototype.order = function(items) {
  var buttons = [],
    emptySpaceItems = [],
    selectionItems = [],
    rightItems = [];

  items.forEach(function(item) {
    // skip separators added dynamically by this class
    if (item.createdBy === this) {
      return;
    }
    if (scout.menus.isButton(item)) {
      buttons.push(item);
    } else if (item.horizontalAlignment === 1) {
      rightItems.push(item);
    } else if (scout.menus.checkType(item, this._menuTypes(this.emptySpaceTypes))) {
      emptySpaceItems.push(item);
    } else if (scout.menus.checkType(item, this._menuTypes(this.selectionTypes))) {
      selectionItems.push(item);
    }
  }, this);

  // add fixed separator between emptySpace and selection
  //FIXME AWE considier visibility of the menus (-> only create separator if there are visible empty space menus)
  if (emptySpaceItems.length > 0 && selectionItems.length > 0) {
    emptySpaceItems.push(this._createSeparator());
  }

  return {
    left: buttons.concat(emptySpaceItems, selectionItems),
    right: rightItems
  };
};

scout.MenuItemsOrder.prototype._menuTypes = function(types) {
  var i, menuTypes = [];
  types = types || [];
  for (i = 0; i < types.length; i++) {
    menuTypes.push(this.objectType + '.' + types[i]);
  }
  return menuTypes;
};

/**
 * The separator here does not exist in the model delivered by the server-side client.
 * The createdBy property is added to the model to find and destroy items added by the UI later.
 */
scout.MenuItemsOrder.prototype._createSeparator = function() {
  return scout.create('Menu', {
    parent: this.session.desktop, // TODO this is not correct, menubar should be the parent
    createdBy: this,
    separator: true
  });
};