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
package org.eclipse.scout.rt.shared.services.common.calendar;

import java.util.Date;

public interface ICalendarItem {

  boolean exists();

  /**
   * mark item as deleted <br>
   * Note: this will not physically delete the item, just set the marker
   */
  void delete();

  long getLastModified();

  void setLastModified(long b);

  String getColor();

  void setColor(String hex);

  /**
   * @return true iff this item covers or intersects the range [minDate,maxDate]
   */
  boolean isIntersecting(Date minDate, Date maxDate);

  /**
   * @return the internal id
   */
  long getId();

  /**
   * set the internal id
   */
  void setId(long id);

  String getSubject();

  void setSubject(String a);

  String getBody();

  void setBody(String a);

  /**
   * @return the user id that is the primary owner of this item
   */
  String getOwner();

  /**
   * set the user id that is the primary owner of this item
   */
  void setOwner(String a);

  RecurrencePattern getRecurrencePattern();

  void setRecurrencePattern(RecurrencePattern p);

}
