/*
 * Copyright (c) BSI Business Systems Integration AG. All rights reserved.
 * http://www.bsiag.com/
 */
package org.eclipse.scout.rt.server.mom;

import org.eclipse.scout.rt.mom.api.AbstractDestinationConfigProperty;
import org.eclipse.scout.rt.mom.api.ClusterMom;
import org.eclipse.scout.rt.mom.api.IDestination;
import org.eclipse.scout.rt.mom.api.IDestination.DestinationType;
import org.eclipse.scout.rt.mom.api.IDestination.IDestinationType;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.server.services.common.clustersync.IClusterNotificationMessage;

/**
 * Definition of destinations used for messaging over {@link ClusterMom}.
 *
 * @since 6.1
 */
public interface IClusterMomDestinations {

  /**
   * Topic to transfer cluster notifications, or <code>null</code> if not configured in <i>'config.properties'</i>.
   */
  IDestination<IClusterNotificationMessage> CLUSTER_NOTIFICATION_TOPIC = CONFIG.getPropertyValue(ClusterSyncTopic.class);

  class ClusterSyncTopic extends AbstractDestinationConfigProperty<IClusterNotificationMessage> {

    @Override
    public String getKey() {
      return "scout.mom.cluster.destination.clusterNotificationTopic";
    }

    @Override
    protected IDestinationType getType() {
      return DestinationType.TOPIC;
    }
  }
}