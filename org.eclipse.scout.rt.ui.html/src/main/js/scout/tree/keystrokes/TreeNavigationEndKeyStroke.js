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
scout.TreeNavigationEndKeyStroke = function(tree, modifierBitMask) {
  scout.TreeNavigationEndKeyStroke.parent.call(this, tree, modifierBitMask);
  this.which = [scout.keys.END];
  this.renderingHints.$drawingArea = function($drawingArea, event) {
    var visibleNodesCount = this.field.visibleNodesFlat.length;
    if(visibleNodesCount>0 && this.field.visibleNodesFlat[visibleNodesCount-1].rendered ){
      return this.field.visibleNodesFlat[visibleNodesCount-1].$node;
    }
    return null;
  }.bind(this);
};
scout.inherits(scout.TreeNavigationEndKeyStroke, scout.AbstractTreeNavigationKeyStroke);

scout.TreeNavigationEndKeyStroke.prototype._handleInternal = function(currentNode) {
  var nodes = this.field.visibleNodesFlat;
  if (nodes.length === 0) {
    return null;
  }
    return nodes[nodes.length-1];
};