/*
 * Copyright (c) BSI Business Systems Integration AG. All rights reserved.
 * http://www.bsiag.com/
 */
package org.eclipse.scout.rt.mom.api;

import java.util.concurrent.TimeUnit;

import org.eclipse.scout.rt.mom.api.encrypter.ClusterEncrypter;
import org.eclipse.scout.rt.mom.api.encrypter.IEncrypter;
import org.eclipse.scout.rt.mom.api.marshaller.BytesMarshaller;
import org.eclipse.scout.rt.mom.api.marshaller.IMarshaller;
import org.eclipse.scout.rt.mom.api.marshaller.JsonMarshaller;
import org.eclipse.scout.rt.mom.api.marshaller.ObjectMarshaller;
import org.eclipse.scout.rt.mom.api.marshaller.TextMarshaller;
import org.eclipse.scout.rt.platform.IPlatform.State;
import org.eclipse.scout.rt.platform.IPlatformListener;
import org.eclipse.scout.rt.platform.config.AbstractClassConfigProperty;
import org.eclipse.scout.rt.platform.config.AbstractStringConfigProperty;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.transaction.TransactionScope;
import org.eclipse.scout.rt.platform.util.IRegistrationHandle;
import org.eclipse.scout.rt.platform.util.concurrent.ThreadInterruptedException;
import org.eclipse.scout.rt.platform.util.concurrent.TimedOutException;

/**
 * Message oriented middleware (MOM) for sending and receiving messages between distributed systems.
 * <p>
 * This MOM provides publish/subscribe (topic-based) or point-to-point (queue-based) messaging. A topic allows to
 * publish a message to multiple subscribers, meaning that the message is transported to all consumers which currently
 * are subscribed. A queue differs from the topic distribution mechanism that the message is transported to exactly one
 * consumer. If there is no subscription the message will be kept until a consumer subscribes for the queue. However, if
 * there are multiple subscriptions for the queue, the message is load balanced to a single consumer.
 * <p>
 * Besides, this MOM provides 'request-reply' messaging, which is kind of synchronous communication between two parties.
 * The publisher of a message blocks until the reply is received. This mode is still based on P2P or pub/sub messaging,
 * meaning there is no open connection for the time of blocking.
 * <p>
 * Message addressing is based on destinations (queues or topics), which additionally allow to register for a
 * {@link IMarshaller}, and optionally for an {@link IEncrypter}. A marshaller is used to transform the transfer object
 * into its transport representation, like text in JSON format, or bytes for the object's serialization data. An
 * encrypter allows end-to-end message encryption, which may be required depending on the messaging topology you choose.
 * However, even if working with a secure transport layer, messages may temporarily be stored like when being delivered
 * to queues - end-to-end encryption ensures confidentiality, integrity, and authenticity of those messages.
 *
 * @since 6.1
 */
public interface IMom {

  /**
   * Subscription mode to acknowledge a message automatically upon its receipt.
   */
  int ACKNOWLEDGE_AUTO = 1;

  /**
   * Subscription mode to acknowledge a message upon successful commit of the receiving transaction.
   */
  int ACKNOWLEDGE_TRANSACTED = 2;

  /**
   * Indicates the order of the MOM's {@link IPlatformListener} to shutdown itself upon entering platform state
   * {@link State#PlatformStopping}. Any listener depending on MOM facility must be configured with an order less than
   * {@link #DESTROY_ORDER}.
   */
  long DESTROY_ORDER = 5_700;

  /**
   * Publishes the given message to the given destination.
   * <p>
   * The message is published with default messaging settings, meaning with normal priority, with persistent delivery
   * mode and without expiration.
   *
   * @param destination
   *          specifies the target of the message, and is either a queue (P2P) or topic (pub/sub). See {@link IMom}
   *          documentation for more information about the difference between topic and queue based messaging.
   * @param transferObject
   *          specifies the transfer object to be sent to the destination.<br>
   *          The object is marshalled into its transport representation using the {@link IMarshaller} registered for
   *          that destination. By default, {@link JsonMarshaller} is used.
   * @see #newQueue(String)
   * @see #newTopic(String)
   * @see #subscribe(IDestination, IMessageListener, RunContext)
   */
  void publish(IDestination destination, Object transferObject);

  /**
   * Publishes the given message to the given destination.
   *
   * @param destination
   *          specifies the target of the message, and is either a queue (P2P) or topic (pub/sub). See {@link IMom}
   *          documentation for more information about the difference between topic and queue based messaging.
   * @param transferObject
   *          specifies the transfer object to be sent to the destination.<br>
   *          The object is marshalled into its transport representation using the {@link IMarshaller} registered for
   *          that destination. By default, {@link JsonMarshaller} is used.
   * @param input
   *          specifies how to publish the message.
   * @see #newQueue(String)
   * @see #newTopic(String)
   * @see #subscribe(IDestination, IMessageListener, RunContext)
   */
  void publish(IDestination destination, Object transferObject, PublishInput input);

  /**
   * Subscribes the given listener to receive messages sent to the given destination. Messages are acknowledged
   * automatically, and which complies with the mode {@link #ACKNOWLEDGE_AUTO}.
   *
   * @param destination
   *          specifies the target to consume messages from, and is either a topic (pub/sub) or queue (P2P). See
   *          {@link IMom} documentation for more information about the difference between topic and queue based
   *          messaging.
   * @param listener
   *          specifies the listener to receive messages.
   * @param runContext
   *          specifies the context in which to receive and process the messages.
   * @return subscription handle to unsubscribe from the destination.
   * @see #publish(IDestination, Object)
   */
  <TRANSFER_OBJECT> ISubscription subscribe(IDestination destination, IMessageListener<TRANSFER_OBJECT> listener, RunContext runContext);

  /**
   * Subscribes the given listener to receive messages sent to the given destination.
   *
   * @param destination
   *          specifies the target to consume messages from, and is either a topic (pub/sub) or queue (P2P). See
   *          {@link IMom} documentation for more information about the difference between topic and queue based
   *          messaging.
   * @param listener
   *          specifies the listener to receive messages.
   * @param runContext
   *          specifies the optional context in which to receive and process the messages. If using transacted
   *          acknowledgment, MOM sets the transaction boundary to {@link TransactionScope#REQUIRES_NEW}.
   * @param acknowledgementMode
   *          specifies the mode how to acknowledge messages. Supported modes are {@link IMom#ACKNOWLEDGE_AUTO} and
   *          {@link IMom#ACKNOWLEDGE_TRANSACTED}.
   * @return subscription handle to unsubscribe from the destination.
   * @see #publish(IDestination, Object)
   */
  <TRANSFER_OBJECT> ISubscription subscribe(IDestination destination, IMessageListener<TRANSFER_OBJECT> listener, RunContext runContext, int acknowledgementMode);

  /**
   * Initiates a 'request-reply' communication with a replier, and blocks until the reply is received (synchronous).
   * <p>
   * This method is for convenience to facilitate synchronous communication between a publisher and a subscriber, and is
   * still based on P2P or pub/sub messaging, meaning that there is no open connection for the time of blocking.
   * <p>
   * Typically, request-reply is used with a queue destination. If using a topic, it is the first reply which is
   * returned.
   * <p>
   * If the current thread is interrupted while waiting for the reply to receive, this method returns with a
   * {@link ThreadInterruptedException} and the interruption is propagated to the consumer(s) as well.
   * <p>
   * The message is published with default messaging settings, meaning with normal priority, with persistent delivery
   * mode and without expiration.
   *
   * @param destination
   *          specifies the target of the message, and is either a queue (P2P) or topic (pub/sub). See {@link IMom}
   *          documentation for more information about the difference between topic and queue based messaging.
   * @param transferObject
   *          specifies the transfer object to be sent to the destination.<br>
   *          The object is marshalled into its transport representation using the {@link IMarshaller} registered for
   *          that destination. By default, {@link JsonMarshaller} is used.
   * @return the reply of the consumer.
   * @throws ThreadInterruptedException
   *           if interrupted while waiting for the reply to receive.
   * @see {@link #reply(IDestination, IRequestListener, RunContext)}
   */
  <REPLY_OBJECT, REQUEST_OBJECT> REPLY_OBJECT request(IDestination destination, REQUEST_OBJECT transferObject);

  /**
   * Initiates a 'request-reply' communication with a replier, and blocks until the reply is received. This type of
   * communication does not support transacted message publishing.
   * <p>
   * This method is for convenience to facilitate synchronous communication between a publisher and a subscriber, and is
   * still based on P2P or pub/sub messaging, meaning that there is no open connection for the time of blocking.
   * <p>
   * Typically, request-reply is used with a queue destination. If using a topic, it is the first reply which is
   * returned.
   * <p>
   * If the current thread is interrupted while waiting for the reply to receive, this method returns with a
   * {@link ThreadInterruptedException} and the interruption is propagated to the consumer(s) as well.
   *
   * @param destination
   *          specifies the target of the message, and is either a queue (P2P) or topic (pub/sub). See {@link IMom}
   *          documentation for more information about the difference between topic and queue based messaging.
   * @param transferObject
   *          specifies the transfer object to be sent to the destination.<br>
   *          The object is marshalled into its transport representation using the {@link IMarshaller} registered for
   *          that destination. By default, {@link JsonMarshaller} is used.
   * @param input
   *          specifies how to publish the message. Transacted publish of the request is not supported.
   * @return the reply of the consumer.
   * @throws ThreadInterruptedException
   *           if interrupted while waiting for the reply to receive. If interrupted, an interruption request is sent to
   *           the consumer(s).
   * @throws TimedOutException
   *           if the timeout specified via {@link PublishInput#withRequestReplyTimeout(long, TimeUnit)} elapsed. If
   *           elapsed, an interruption request is sent to the consumer(s).
   * @see #reply(IDestination, IRequestListener, RunContext)
   */

  <REPLY_OBJECT> REPLY_OBJECT request(IDestination destination, Object transferObject, PublishInput input);

  /**
   * Subscribes the given listener to receive messages from 'request-reply' communication sent to the given destination.
   *
   * @param destination
   *          specifies the target to consume messages from, and is either a topic (pub/sub) or queue (P2P). See
   *          {@link IMom} documentation for more information about the difference between topic and queue based
   *          messaging.
   * @param listener
   *          specifies the listener to receive messages.
   * @param runContext
   *          specifies the context in which to receive and process the messages.
   * @return subscription handle to unsubscribe from the destination.
   * @see #request(IDestination, Object)
   */
  <REQUEST_TRANSFER_OBJECT, REPLY_TRANSFER_OBJECT> ISubscription reply(IDestination destination, IRequestListener<REQUEST_TRANSFER_OBJECT, REPLY_TRANSFER_OBJECT> listener, RunContext runContext);

  /**
   * Creates a new topic for publish/subscribe messaging.
   */
  IDestination newTopic(String topic);

  /**
   * Creates a new queue for point-to-point messaging.
   */
  IDestination newQueue(String queue);

  /**
   * Looks up the given destination using JNDI.
   */
  IDestination lookupDestination(String destination);

  /**
   * Creates a input to control how to publish a message. The input returned specifies normal delivery priority,
   * persistent delivery mode, and without expiration.
   */
  PublishInput newPublishInput();

  /**
   * Registers a marshaller for transfer objects sent to the given destination, or which are received from the given
   * destination.
   * <p>
   * A marshaller transforms a transfer object into its transport representation to be sent across the network.
   * <p>
   * By default, if a destination does not specify a marshaller, {@link JsonMarshaller} is used.
   *
   * @return registration handle to unregister the marshaller from the destination.
   * @see TextMarshaller
   * @see BytesMarshaller
   * @see JsonMarshaller
   * @see ObjectMarshaller
   */
  IRegistrationHandle registerMarshaller(IDestination destination, IMarshaller marshaller);

  /**
   * Allows end-to-end encryption for transfer objects sent to the given destination. By default, no encryption is used.
   *
   * @return registration handle to unregister the encrypter from the destination.
   * @see ClusterEncrypter
   */
  IRegistrationHandle registerEncrypter(IDestination destination, IEncrypter encrypter);

  /**
   * Destroys this MOM and releases all associated resources.
   */
  void destroy();

  /**
   * Specifies the default {@link IEncrypter} to use if no encrypter is specified for a destination.
   * <p>
   * By default, no encrypter is used.
   */
  class EncrypterProperty extends AbstractClassConfigProperty<IEncrypter> {

    @Override
    public String getKey() {
      return "scout.mom.encrypter";
    }
  }

  /**
   * Specifies the default {@link IMarshaller} to use if no marshaller is specified for a destination.
   * <p>
   * By default, {@link JsonMarshaller} is used.
   */
  class MarshallerProperty extends AbstractClassConfigProperty<IMarshaller> {

    @Override
    public String getKey() {
      return "scout.mom.marshaller";
    }

    @Override
    protected Class<? extends IMarshaller> getDefaultValue() {
      return JsonMarshaller.class;
    }
  }

  /**
   * Specifies the topic to receive cancellation request for request-reply communication.
   * <p>
   * By default, the topic 'scout.mom.requestreply.cancellation' is used.
   */
  class RequestReplyCancellationTopicProperty extends AbstractStringConfigProperty {

    @Override
    public String getKey() {
      return "scout.mom.requestreply.cancellation.topic";
    }

    @Override
    protected String getDefaultValue() {
      return "scout.mom.requestreply.cancellation";
    }
  }
}