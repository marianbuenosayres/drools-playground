package com.plugtree.haksession.registries;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitAcksMessageListener implements MessageListener {
		
	private static final Logger log = LoggerFactory.getLogger(WaitAcksMessageListener.class);
	
	private AtomicInteger howManyAcks;
	
	public WaitAcksMessageListener(MessageConsumer consumer, int howManyAcks) {
		this.howManyAcks = new AtomicInteger(howManyAcks);
	}
	
	@Override
	public void onMessage(Message message) {
		howManyAcks.decrementAndGet();
	}
	
	public void waitForCompletion(long timeout) {
		long timeStart = System.currentTimeMillis();
		long timeDiff = 0;
		while (howManyAcks.get() > 0 && timeDiff < timeout) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
			timeDiff = System.currentTimeMillis() - timeStart;
		}
		if (howManyAcks.get() > 0) {
			log.warn("JMSKieSessionRegistry acknowledge wait timeout. " +
					"Could it be that a node failed?");
		}
	}
}
