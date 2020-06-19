package com.example.vet.worker;

import com.example.vet.config.ActiveMQConfig;
import com.example.vet.config.EventBusConfig;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.jms.*;

public class VetUserCreator extends AbstractVerticle implements MessageListener, ExceptionListener {

    private Session session;
    private Connection connection;
    private com.example.vet.service.reactivex.VetESService dbService;
    private static final Logger logger = LoggerFactory.getLogger(VetUserCreator.class);

    @Override
    public void start() throws Exception {
        dbService = com.example.vet.service.VetESService.createProxy(vertx.getDelegate(), EventBusConfig.VET_DB_QUEUE.address);
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost");
        connection = factory.createConnection();
        connection.setExceptionListener(this);
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(ActiveMQConfig.USER_CREATION_QUEUE.address);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
        logger.info("Connection established");
    }

    @Override
    public void stop() throws Exception {
        session.close();
        connection.close();
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            try {
                JsonObject user = new JsonObject(textMessage.getText());
                if (user.containsKey("password")) {
                    DeliveryOptions options = new DeliveryOptions().addHeader("action", "encode");
                    vertx.eventBus()
                        .rxRequest(EventBusConfig.PASSWORD_ENCODER_QUEUE.address, user.getString("password"), options)
                        .flatMap(hashed -> {
                            user.put("password", hashed.body());
                            return dbService.rxSave(user);
                        })
                        .subscribe(this::onSaved, this::onFailed);
                    return;
                }
                String name = user.getString("name");
                String email = user.getString("email");
                logger.warn("User {}:{} missing password field. Shouldn't be in the queue", name, email);
                dbService.rxSave(user).subscribe(this::onSaved, this::onFailed);
            } catch (JMSException e) {
                logger.error("Cannot get text from message ", e);
            }
        } else {
            logger.warn("Invalid message type: not a text: {}", message);
        }
    }

    private void onSaved(JsonObject user) {
        logger.info("saved: {}", user.getString("_id"));
    }

    private void onFailed(Throwable error) {
        logger.error("save failed: ", error);
    }

    @Override
    public void onException(JMSException exception) {
        logger.error(MarkerFactory.getMarker("FATAL"), "JMS Exception occurred.  Shutting down client.", exception);
    }
}
