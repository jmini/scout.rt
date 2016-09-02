package org.eclipse.scout.rt.mom.jms;

import static org.eclipse.scout.rt.mom.jms.IJmsMomProperties.PROP_REPLY_ID;
import static org.eclipse.scout.rt.platform.util.Assertions.assertFalse;
import static org.eclipse.scout.rt.platform.util.Assertions.assertNotNull;

import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.scout.rt.mom.api.IBiDestination;
import org.eclipse.scout.rt.mom.api.IDestination;
import org.eclipse.scout.rt.mom.api.IMessageListener;
import org.eclipse.scout.rt.mom.api.IMom;
import org.eclipse.scout.rt.mom.api.IMomImplementor;
import org.eclipse.scout.rt.mom.api.IRequestListener;
import org.eclipse.scout.rt.mom.api.ISubscription;
import org.eclipse.scout.rt.mom.api.PublishInput;
import org.eclipse.scout.rt.mom.api.encrypter.IEncrypter;
import org.eclipse.scout.rt.mom.api.marshaller.IMarshaller;
import org.eclipse.scout.rt.mom.api.marshaller.TextMarshaller;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.platform.IPlatform.State;
import org.eclipse.scout.rt.platform.IPlatformListener;
import org.eclipse.scout.rt.platform.Order;
import org.eclipse.scout.rt.platform.Platform;
import org.eclipse.scout.rt.platform.PlatformEvent;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.ApplicationNameProperty;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.ApplicationVersionProperty;
import org.eclipse.scout.rt.platform.context.CorrelationId;
import org.eclipse.scout.rt.platform.context.NodeIdentifier;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.exception.DefaultRuntimeExceptionTranslator;
import org.eclipse.scout.rt.platform.exception.ExceptionHandler;
import org.eclipse.scout.rt.platform.exception.PlatformException;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.eclipse.scout.rt.platform.job.IBlockingCondition;
import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.transaction.ITransaction;
import org.eclipse.scout.rt.platform.util.IRegistrationHandle;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.platform.util.concurrent.IFunction;
import org.eclipse.scout.rt.platform.util.concurrent.ThreadInterruptedException;
import org.eclipse.scout.rt.platform.util.concurrent.ThreadInterruption;
import org.eclipse.scout.rt.platform.util.concurrent.ThreadInterruption.IRestorer;
import org.eclipse.scout.rt.platform.util.concurrent.TimedOutException;
import org.eclipse.scout.rt.platform.util.date.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of 'instance-scoped' {@link IMom} based on JMS (Java Messaging Standard).
 * <p>
 * This class expects a JMS implementor to be available at runtime.
 *
 * @since 6.1
 */
@Bean
public class JmsMomImplementor implements IMomImplementor {

  private static final Logger LOG = LoggerFactory.getLogger(JmsMomImplementor.class);

  protected final String m_momUid = UUID.randomUUID().toString();
  protected Context m_context;
  protected Connection m_connection;

  protected Session m_defaultSession; // single-threaded
  protected MessageProducer m_defaultProducer;

  protected TemporaryQueue m_replyQueue;
  protected Topic m_requestReplyCancellationTopic;
  protected final Map<String, ReplyFuture> m_replyFutureMap = new ConcurrentHashMap<>();
  protected final Replier m_replier = BEANS.get(Replier.class).init(this);

  protected final Map<IDestination, IMarshaller> m_marshallers = new ConcurrentHashMap<>();
  protected final Map<IDestination, IEncrypter> m_encrypters = new ConcurrentHashMap<>();

  protected final Map<Integer, ISubscriptionStrategy> m_subscriptionStrategies = new ConcurrentHashMap<>();

  protected IMarshaller m_defaultMarshaller;
  protected IEncrypter m_defaultEncrypter;

  @Override
  public void init(final Map<Object, Object> properties) throws Exception {
    final String symbolicName = StringUtility.nvl(properties.get(SYMBOLIC_NAME), "MOM");
    if (Platform.get().inDevelopmentMode()) {
      LOG.info("{} configuration: {}", symbolicName, properties);
    }

    m_context = createContext(properties);
    m_connection = createConnection(m_context, properties);
    m_defaultSession = m_connection.createSession(false /* non-transacted */, Session.AUTO_ACKNOWLEDGE);
    m_defaultProducer = m_defaultSession.createProducer(null /* all destinations */);

    m_subscriptionStrategies.put(ACKNOWLEDGE_AUTO, BEANS.get(AutoAcknowledgeStrategy.class).init(this, false));
    m_subscriptionStrategies.put(ACKNOWLEDGE_AUTO_SINGLE_THREADED, BEANS.get(AutoAcknowledgeStrategy.class).init(this, true));
    m_subscriptionStrategies.put(ACKNOWLEDGE_TRANSACTED, BEANS.get(TransactedStrategy.class).init(this));

    // Register consumer to dispatch replies of 'request-reply' messaging to the requester.
    m_replyQueue = m_defaultSession.createTemporaryQueue();
    m_defaultSession
        .createConsumer(m_replyQueue)
        .setMessageListener(new JmsMessageListener() {

          @Override
          public void onJmsMessage(final Message message) throws JMSException {
            final String replyId = message.getStringProperty(PROP_REPLY_ID);
            final ReplyFuture replyFuture = m_replyFutureMap.remove(replyId);
            if (replyFuture != null) {
              replyFuture.set(message);
            }
          }
        });

    // Register consumer to handle cancellation requests for 'request-reply' communication.
    m_requestReplyCancellationTopic = m_defaultSession.createTopic(CONFIG.getPropertyValue(RequestReplyCancellationTopicProperty.class));
    m_defaultSession
        .createConsumer(m_requestReplyCancellationTopic)
        .setMessageListener(new JmsMessageListener() {

          @Override
          public void onJmsMessage(final Message message) throws JMSException {
            Jobs.getJobManager().cancel(Jobs.newFutureFilterBuilder()
                .andMatchExecutionHint(message.getStringProperty(PROP_REPLY_ID))
                .toFilter(), true);
          }
        });

    // Set default marshaller
    m_defaultMarshaller = BEANS.get(CONFIG.getPropertyValue(MarshallerProperty.class));

    // Set default encrypter
    final Class<? extends IEncrypter> encrypterClazz = CONFIG.getPropertyValue(EncrypterProperty.class);
    m_defaultEncrypter = (encrypterClazz != null ? BEANS.get(encrypterClazz) : null);

    // Start the connection
    m_connection.start();
    LOG.info("{} initialized: {}", symbolicName, m_connection);
  }

  @Override
  public <DTO> void publish(final IDestination<DTO> destination, final DTO transferObject, final PublishInput input) {
    assertNotNull(destination, "destination not specified");
    assertNotNull(input, "publishInput not specified");

    try {
      if (input.isTransactional()) {
        publishTransactional(destination, transferObject, input);
      }
      else {
        publishNonTransactional(destination, transferObject, input);
      }
    }
    catch (final JMSException | GeneralSecurityException e) {
      throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e);
    }
  }

  protected <DTO> void publishNonTransactional(final IDestination<DTO> destination, final DTO transferObject, final PublishInput input) throws JMSException, GeneralSecurityException {
    final IMarshaller marshaller = lookupMarshaller(destination);
    final IEncrypter encrypter = lookupEncrypter(destination);

    final Message message = JmsMessageWriter.newInstance(m_defaultSession, marshaller, encrypter)
        .writeTransferObject(transferObject)
        .writeProperties(input.getProperties(), true)
        .writeCorrelationId(CorrelationId.CURRENT.get())
        .build();
    send(m_defaultProducer, lookupJmsDestination(destination, m_defaultSession), message, toJmsDeliveryMode(input), toJmsPriority(input), toJmsTimeToLive(input));
  }

  protected <DTO> void publishTransactional(final IDestination<DTO> destination, final DTO transferObject, final PublishInput input) throws JMSException, GeneralSecurityException {
    final ITransaction currentTransaction = assertNotNull(ITransaction.CURRENT.get(), "Transaction required for transactional messaging");
    final IMarshaller marshaller = lookupMarshaller(destination);
    final IEncrypter encrypter = lookupEncrypter(destination);

    // Register transaction member for transacted publishing.
    final JmsTransactionMember txMember = currentTransaction.registerMemberIfAbsent(m_momUid, new IFunction<String, JmsTransactionMember>() {

      @Override
      public JmsTransactionMember apply(final String memberId) {
        try {
          final Session transactedSession = m_connection.createSession(true, Session.SESSION_TRANSACTED);
          return BEANS.get(JmsTransactionMember.class)
              .withMemberId(memberId)
              .withTransactedSession(transactedSession)
              .withTransactedProducer(transactedSession.createProducer(null))
              .withAutoClose(true); // close upon transaction end
        }
        catch (final JMSException e) {
          throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e);
        }
      }
    });

    // Publish the message
    final Session transactedSession = txMember.getTransactedSession();
    final MessageProducer transactedProducer = txMember.getTransactedProducer();

    final Message message = JmsMessageWriter.newInstance(transactedSession, marshaller, encrypter)
        .writeTransferObject(transferObject)
        .writeProperties(input.getProperties(), true)
        .writeCorrelationId(CorrelationId.CURRENT.get())
        .build();
    send(transactedProducer, lookupJmsDestination(destination, transactedSession), message, toJmsDeliveryMode(input), toJmsPriority(input), toJmsTimeToLive(input));
  }

  @Override
  public <DTO> ISubscription subscribe(final IDestination<DTO> destination, final IMessageListener<DTO> listener, final RunContext runContext, final int acknowledgementMode) {
    assertNotNull(destination, "destination not specified");
    assertNotNull(listener, "messageListener not specified");

    try {
      final ISubscriptionStrategy strategy = assertNotNull(m_subscriptionStrategies.get(acknowledgementMode), "Acknowledgement mode not supported [mode={}]", acknowledgementMode);
      return strategy.subscribe(destination, listener, runContext != null ? runContext : RunContexts.empty());
    }
    catch (final JMSException e) {
      throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e);
    }
  }

  @Override
  public <REQUEST, REPLY> REPLY request(final IBiDestination<REQUEST, REPLY> destination, final REQUEST requestObject, final PublishInput input) {
    assertNotNull(destination, "destination not specified");
    assertNotNull(input, "publishInput not specified");
    assertFalse(input.isTransactional(), "transactional mode not supported for 'request-reply' communication");

    final IMarshaller marshaller = lookupMarshaller(destination);
    final IEncrypter encrypter = lookupEncrypter(destination);

    // Prepare to receive the reply message
    final String replyId = String.format("scout.mom.requestreply.uid-%s", UUID.randomUUID()); // JMS message ID not applicable because unknown until sent
    final ReplyFuture<REPLY> replyFuture = new ReplyFuture<>(marshaller, encrypter, replyId);

    m_replyFutureMap.put(replyId, replyFuture);
    try {
      // Prepare the request message
      final Message message = JmsMessageWriter.newInstance(m_defaultSession, marshaller, encrypter)
          .writeReplyTo(m_replyQueue)
          .writeReplyId(replyId)
          .writeProperties(input.getProperties(), true)
          .writeCorrelationId(CorrelationId.CURRENT.get())
          .writeTransferObject(requestObject)
          .build();
      send(m_defaultProducer, lookupJmsDestination(destination, m_defaultSession), message, toJmsDeliveryMode(input), toJmsPriority(input), toJmsTimeToLive(input));

      // Wait until the reply is received
      return waitForReply(replyFuture, input.getRequestReplyTimeout());
    }
    catch (final JMSException | GeneralSecurityException e) {
      throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e);
    }
    finally {
      m_replyFutureMap.remove(replyId);
    }
  }

  /**
   * Waits until received the reply. If interrupted or the timeout elapses, the request is cancelled.
   */
  protected <REPLY> REPLY waitForReply(final ReplyFuture<REPLY> replyFuture, final long timeout) throws JMSException, GeneralSecurityException {
    try {
      return replyFuture.awaitDoneAndGet(timeout);
    }
    catch (final ExecutionException e) {
      throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e); // exception thrown by the replier
    }
    catch (ThreadInterruptedException | TimedOutException e) {
      final Message cancellationMessage = JmsMessageWriter.newInstance(m_defaultSession, BEANS.get(TextMarshaller.class), null)
          .writeReplyId(replyFuture.getReplyId())
          .writeCorrelationId(CorrelationId.CURRENT.get())
          .build();
      send(m_defaultProducer, m_requestReplyCancellationTopic, cancellationMessage, Message.DEFAULT_DELIVERY_MODE, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
      throw e;
    }
  }

  @Override
  public <REQUEST, REPLY> ISubscription reply(final IBiDestination<REQUEST, REPLY> destination, final IRequestListener<REQUEST, REPLY> listener, final RunContext runContext) {
    assertNotNull(destination, "Destination not specified");
    assertNotNull(listener, "MessageListener not specified");

    try {
      return m_replier.subscribe(destination, listener, runContext != null ? runContext : RunContexts.empty());
    }
    catch (final JMSException e) {
      throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e);
    }
  }

  @Override
  public void destroy() {
    try {
      m_connection.close();
    }
    catch (final JMSException e) {
      LOG.error("Failed to destory MOM", e);
    }
  }

  @Override
  public IRegistrationHandle registerMarshaller(final IDestination<?> destination, final IMarshaller marshaller) {
    m_marshallers.put(destination, marshaller);
    return new IRegistrationHandle() {

      @Override
      public void dispose() {
        m_marshallers.remove(destination);
      }
    };
  }

  @Override
  public IRegistrationHandle registerEncrypter(final IDestination<?> destination, final IEncrypter encrypter) {
    m_encrypters.put(destination, encrypter);
    return new IRegistrationHandle() {

      @Override
      public void dispose() {
        m_encrypters.remove(encrypter);
      }
    };
  }

  /**
   * Registers the given {@link ISubscriptionStrategy} to receive message.
   *
   * @see AutoAcknowledgeStrategy
   * @see TransactedStrategy
   */
  public void registerSubscriptionStrategy(final int acknowledgementMode, final ISubscriptionStrategy strategy) {
    m_subscriptionStrategies.put(acknowledgementMode, strategy);
  }

  /**
   * Returns the {@link IMarshaller} registered for the given destination, and is never <code>null</code>.
   */
  public IMarshaller lookupMarshaller(final IDestination<?> destination) {
    final IMarshaller marshaller = m_marshallers.get(destination);
    if (marshaller != null) {
      return marshaller;
    }
    return m_defaultMarshaller;
  }

  /**
   * Returns the {@link IEncrypter} registered for the given destination, or <code>null</code> if not set.
   */
  public IEncrypter lookupEncrypter(final IDestination<?> destination) {
    final IEncrypter encrypter = m_encrypters.get(destination);
    if (encrypter != null) {
      return encrypter;
    }

    return m_defaultEncrypter;
  }

  /**
   * Returns the {@link Connection} to the JMS broker.
   */
  public Connection getConnection() {
    return m_connection;
  }

  /**
   * Returns the default {@link Session} of this MOM to receive and send messages. This session is not transacted, and
   * configured for auto-acknowledge messages.
   * <p>
   * Please note: A JMS session is single-threaded, so while processing a message, not other message can be received,
   * nor a message published.
   */
  public Session getDefaultSession() {
    return m_defaultSession;
  }

  /**
   * Returns the default {@link MessageProducer} of this MOM to send messages. There is not destination associated with
   * this producer.
   */
  public MessageProducer getDefaultProducer() {
    return m_defaultProducer;
  }

  /**
   * Sends the message to the given destination.
   */
  public void send(final MessageProducer producer, final Destination destination, final Message message, final int deliveryMode, final int priority, final long timeToLive) throws JMSException {
    final IRestorer interruption = ThreadInterruption.clear(); // Temporarily clear the thread's interrupted status while sending a message.
    try {
      LOG.debug("Sending JMS message [destination={}, message={}]", destination, message);
      producer.send(destination, message, deliveryMode, priority, timeToLive);
    }
    finally {
      interruption.restore();
    }
  }

  protected Context createContext(final Map<Object, Object> properties) throws NamingException {
    return new InitialContext(new Hashtable<>(properties));
  }

  protected Connection createConnection(final Context context, final Map<Object, Object> properties) throws NamingException, JMSException {
    final String connectionFactoryName = (String) assertNotNull(properties.get(CONNECTION_FACTORY), "Property {} not specified to lookup connection factory", CONNECTION_FACTORY);
    final ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(connectionFactoryName);
    final Connection connection = connectionFactory.createConnection();
    connection.setClientID(computeClientId(properties));
    connection.setExceptionListener(new ExceptionListener() {

      @Override
      public void onException(final JMSException exception) {
        BEANS.get(MomExceptionHandler.class).handle(exception);
      }
    });
    return connection;
  }

  public Destination lookupJmsDestination(final IDestination<?> destination, final Session session) {
    try {
      switch (destination.getType()) {
        case IDestination.TOPIC:
          return session.createTopic(destination.getName());
        case IDestination.QUEUE:
          return session.createQueue(destination.getName());
        case IDestination.JNDI_LOOKUP:
          return (Destination) m_context.lookup(destination.getName());
        default:
          throw new PlatformException("Not supported destination [type={}]", destination.getType());
      }
    }
    catch (final JMSException | NamingException e) {
      throw BEANS.get(DefaultRuntimeExceptionTranslator.class).translate(e);
    }
  }

  public int toJmsPriority(final PublishInput publishInput) {
    return publishInput.getPriority() + Message.DEFAULT_PRIORITY;
  }

  public long toJmsTimeToLive(final PublishInput publishInput) {
    return publishInput.getTimeToLive() == PublishInput.INFINITELY ? Message.DEFAULT_TIME_TO_LIVE : publishInput.getTimeToLive();
  }

  public int toJmsDeliveryMode(final PublishInput publishInput) {
    return publishInput.getDeliveryMode() == PublishInput.DELIVERY_MODE_PERSISTENT ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
  }

  /**
   * Returns the identifier to name the {@link Connection}.
   */
  protected String computeClientId(final Map<Object, Object> properties) {
    final String applicationName = CONFIG.getPropertyValue(ApplicationNameProperty.class);
    final String applicationVersion = CONFIG.getPropertyValue(ApplicationVersionProperty.class);
    final String nodeId = BEANS.get(NodeIdentifier.class).get();
    final String now = DateUtility.format(new Date(), "yyyy-MM-dd HH:mm:ss,SSS");
    return String.format("%s [application='%s:%s', nodeId='%s', created on=%s]", StringUtility.nvl(properties.get(SYMBOLIC_NAME), "MOM"), applicationName, applicationVersion, nodeId, now);
  }

  /**
   * Future to wait for a reply to receive.
   */
  protected static class ReplyFuture<REPLY> {

    protected final IBlockingCondition m_condition = Jobs.newBlockingCondition(true);
    private final String m_replyId;
    private final IMarshaller m_marshaller;
    private final IEncrypter m_encrypter;

    protected volatile Message m_reply;

    public ReplyFuture(final IMarshaller marshaller, final IEncrypter encrypter, final String replyId) {
      m_marshaller = marshaller;
      m_encrypter = encrypter;
      m_replyId = replyId;
    }

    /**
     * Sets the reply.
     */
    public void set(final Message reply) {
      m_reply = reply;
      m_condition.setBlocking(false);
    }

    /**
     * Waits until a reply is received.
     *
     * @param timeoutMillis
     *          the maximal time to wait for the job to complete, or {@link PublishInput#INFINITELY} to wait infinitely.
     * @throws ThreadInterruptedException
     *           if interrupted while waiting.
     * @throws TimedOutException
     *           if the wait timed out.
     */
    @SuppressWarnings("unchecked")
    public REPLY awaitDoneAndGet(final long timeoutMillis) throws JMSException, GeneralSecurityException, ExecutionException {
      // Wait until the reply is received
      if (timeoutMillis == PublishInput.INFINITELY) {
        m_condition.waitFor();
      }
      else {
        m_condition.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
      }

      // Evaluate the reply
      final JmsMessageReader reader = JmsMessageReader.newInstance(m_reply, m_marshaller, m_encrypter);
      final Object reply = reader.readMessage().getTransferObject();
      if (reader.readRequestReplySuccess()) {
        return (REPLY) reply;
      }
      final Throwable cause = reply instanceof Throwable ? (Throwable) reply : new ProcessingException("Request-Reply failed");
      throw new ExecutionException(cause);
    }

    public String getReplyId() {
      return m_replyId;
    }
  }

  public static class MomExceptionHandler extends ExceptionHandler {

    @Override
    public void handle(final Throwable t) {
      if (t instanceof GeneralSecurityException) {
        LOG.warn("Decryption failed, either because of a bad authenticity token or a cipher problem. Message is discarded.", t);
      }
      else {
        super.handle(t);
      }
    }
  }

  /**
   * {@link IPlatformListener} to shutdown this MOM upon platform shutdown.
   */
  @Order(IMom.DESTROY_ORDER)
  public static class MomPlatformListener implements IPlatformListener {

    @Override
    public void stateChanged(final PlatformEvent event) {
      if (State.PlatformStopping.equals(event.getState())) {
        BEANS.get(JmsMomImplementor.class).destroy();
      }
    }
  }

  @Override
  public void setDefaultMarshaller(final IMarshaller marshaller) {
    m_defaultMarshaller = marshaller;
  }

  @Override
  public void setDefaultEncrypter(final IEncrypter encrypter) {
    m_defaultEncrypter = encrypter;
  }
}