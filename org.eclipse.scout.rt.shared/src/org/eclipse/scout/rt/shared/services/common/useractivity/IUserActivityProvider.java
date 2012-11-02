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
package org.eclipse.scout.rt.shared.services.common.useractivity;

import java.beans.PropertyChangeListener;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.service.IService;

/**
 * The implementation of this provider is os specific and provided as a fragment
 * or os specific plugin. The implementation is registered as service
 */
@Priority(-3)
public interface IUserActivityProvider extends IService, IPropertyObserver {
  /**
   * property name used in {@link PropertyChangeListener}
   */
  String PROP_ACTIVE = "active";

  /**
   * @return true if the user is active in any way (mouse, keyboard, other)
   */
  boolean isActive();

}
