package com.plugtree.haksession.registries;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.naming.Context;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plugtree.haksession.HAKieSessionRegistry;

public class JMSKieSessionRegistry implements HAKieSessionRegistry, MessageListener {

	private static final Logger log = LoggerFactory.getLogger(JMSKieSessionRegistry.class);
	
	public static final String CONNECTION_FACTORY_JNDI = "connection.factory.jndi";
	public static final String TOPIC_NAME = "topic.name";
	public static final String NODE_IDENTIFIER = "node.identifier";
	public static final String STORE_X_FIRINGS = "store.x.firings";
	public static final String HOW_MANY_NODES = "how.many.nodes";
	public static final String FAIL_ON_ERROR = "fail.on.error";
	
	private static final int RULE_FIRED = 1;
	private static final int FACT_INSERT = 2;
	private static final int FACT_RETRACT = 3;
	private static final int FACT_UPDATE = 4;
	
	private final List<RuleExecution> executions = new LinkedList<RuleExecution>();
	private final Map<Object, Object> markedWMChanges = new WeakHashMap<Object, Object>();
	private KieSession ksession; 
	
	private ConnectionFactory factory;
	private Connection connection;
	private Session session;
	private Topic topic;
	private TopicSubscriber subscriber;
	private MessageProducer producer; 
	private String nodeId;
	private int amountOfStoredFirings;
	private int howManyAcks;
	private boolean failOnError;
	
	public JMSKieSessionRegistry(Context context, Properties connProperties) {
		this.nodeId = connProperties.getProperty(NODE_IDENTIFIER);
		this.amountOfStoredFirings = Integer.valueOf(connProperties.getProperty(STORE_X_FIRINGS));
		this.howManyAcks = Integer.valueOf(connProperties.getProperty(HOW_MANY_NODES)) - 1;
		this.failOnError = Boolean.valueOf(connProperties.getProperty(FAIL_ON_ERROR));
		try {
			this.factory = (ConnectionFactory) context.lookup(connProperties.getProperty(CONNECTION_FACTORY_JNDI));
			this.connection = factory.createConnection();
			this.connection.setClientID(this.nodeId);
			this.session = connection.createSession(true, Session.SESSION_TRANSACTED);
			this.topic = session.createTopic(connProperties.getProperty(TOPIC_NAME));
			this.subscriber = session.createDurableSubscriber(topic, nodeId, null, true);
			this.producer = session.createProducer(topic);
			subscriber.setMessageListener(this);
			this.connection.start();
		} catch (Exception e) {
			throw new RuntimeException("Problem initializing JMSKieSessionRegistry", e);
		}
	}

	@Override
	public void setKieSession(KieSession ksession) {
		this.ksession = ksession;
	}
	
	@Override
	public void onMessage(Message msg) {
		try {
			StreamMessage stmsg = (StreamMessage) msg;
			int type = stmsg.readInt();
			switch (type) {
			case RULE_FIRED: handleRuleFired(stmsg);
				break;
			case FACT_INSERT: handleFactInsert(stmsg);
				break;
			case FACT_RETRACT: handleFactRetract(stmsg);
				break;
			case FACT_UPDATE: handleFactUpdate(stmsg);
				break;
			default:
				if (failOnError) {
					throw new RuntimeException("Problem reading message in JMSKieSessionRegistry: Unknown type " + type);
				}
			}
			
			Destination replyTo = msg.getJMSReplyTo();
			MessageProducer replier = session.createProducer(replyTo);
			replier.send(session.createTextMessage("ACK"));
			replier.close();
			session.commit();
		} catch (JMSException e) {
			log.error("Problem receiving message on JMSKieSessionRegistry", e);
			if (failOnError) {
				throw new RuntimeException("Problem receiving message on JMSKieSessionRegistry", e);
			}
		}
	}

	private void handleFactUpdate(StreamMessage stmsg) throws JMSException {
		Object factToUpdate = stmsg.readObject();
		Object factUpdated = stmsg.readObject();
		if (markedWMChanges.containsKey(factUpdated)) {
			markedWMChanges.remove(factUpdated);
		} else if (ksession != null) {
			FactHandle handle = ksession.getFactHandle(factToUpdate);
			if (handle != null) {
				ksession.update(handle, factUpdated);
			}
			markedWMChanges.put(factUpdated, "");
		}
	}

	private void handleFactRetract(StreamMessage stmsg) throws JMSException {
		Object factToRetract = stmsg.readObject();
		if (markedWMChanges.containsKey(factToRetract)) {
			markedWMChanges.remove(factToRetract);
		} else if (ksession != null) {
			FactHandle handle = ksession.getFactHandle(factToRetract);
			if (handle != null) {
				ksession.delete(handle);
			}
			markedWMChanges.put(factToRetract, "");
		}
	}

	private void handleFactInsert(StreamMessage stmsg) throws JMSException {
		Object factToInsert = stmsg.readObject();
		if (markedWMChanges.containsKey(factToInsert)) {
			markedWMChanges.remove(factToInsert);
		} else if (ksession != null) {
			FactHandle handle = ksession.getFactHandle(factToInsert);
			if (handle == null) {
				ksession.insert(factToInsert);
			}
			markedWMChanges.put(factToInsert, "");
		}
	}

	private void handleRuleFired(StreamMessage stmsg) throws JMSException {
		String ruleName = null;
		int size = stmsg.readInt();
		List<Object> facts = new ArrayList<Object>(size);
		for (int index = 0; index < size; index++) {
			Object obj = stmsg.readObject();
			facts.add(obj);
		}
		ruleName = stmsg.readString();
		while (executions.size() > amountOfStoredFirings) {
			executions.remove(0);
		}
		executions.add(new RuleExecution(ruleName, facts));
	}

	protected void finalize() throws Throwable {
		this.session.unsubscribe(this.nodeId);
		this.connection.stop();
		super.finalize();
	}
	
	@Override
	public boolean hasFiredRuleForObjects(String ruleName, List<Object> facts) {
		RuleExecution check = new RuleExecution(ruleName, facts);
		return executions.contains(check);
	}

	@Override
	public void ruleFiredForObjects(String ruleName, List<Object> facts) {
		try {
			StreamMessage msg = session.createStreamMessage();
			msg.writeInt(RULE_FIRED);
			msg.writeInt(facts.size());
			for (Object obj : facts) {
				msg.writeObject(obj);
			}
			msg.writeString(ruleName);
			Queue replyTo = session.createTemporaryQueue();
			MessageConsumer consumer = session.createConsumer(replyTo);
			WaitAcksMessageListener listener = new WaitAcksMessageListener(consumer, howManyAcks);
			consumer.setMessageListener(listener);
			msg.setJMSReplyTo(replyTo);
			producer.send(msg);
			session.commit();
			//receive acknowledge from all other clients
			listener.waitForCompletion(2000);
		} catch (JMSException e) {
			log.error("Problem sending message on JMSKieSessionRegistry", e);
			if (failOnError) {
				throw new RuntimeException("Problem sending message on JMSKieSessionRegistry", e);
			}
		}
	}
	
	private void factMessage(int type, Object... facts) {
		try {
			StreamMessage msg = session.createStreamMessage();
			msg.writeInt(type);
			for (Object fact : facts) {
				msg.writeObject(fact);
			}
			Queue replyTo = session.createTemporaryQueue();
			MessageConsumer consumer = session.createConsumer(replyTo);
			WaitAcksMessageListener listener = new WaitAcksMessageListener(consumer, howManyAcks);
			consumer.setMessageListener(listener);
			msg.setJMSReplyTo(replyTo);
			producer.send(msg);
			session.commit();
			//receive acknowledge from all other clients
			listener.waitForCompletion(2000);
		} catch (JMSException e) {
			log.error("Problem sending message on JMSKieSessionRegistry", e);
			if (failOnError) {
				throw new RuntimeException("Problem sending message on JMSKieSessionRegistry", e);
			}
		}
	}
	
	@Override
	public void factInserted(Object fact) {
		markedWMChanges.put(fact, "");
		factMessage(FACT_INSERT, fact);
	}

	@Override
	public void factRetracted(Object fact) {
		markedWMChanges.put(fact, "");
		factMessage(FACT_RETRACT, fact);
	}

	@Override
	public void factUpdated(Object oldFact, Object newFact) {
		markedWMChanges.put(newFact, "");
		factMessage(FACT_UPDATE, oldFact, newFact);
	}
}
