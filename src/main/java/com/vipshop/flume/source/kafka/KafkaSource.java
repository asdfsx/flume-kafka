package com.vipshop.flume.source.kafka;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vipshop.flume.KafkaUtil;

public class KafkaSource extends AbstractSource implements Configurable,
		PollableSource {

	/**
	 * @param args
	 */
	private static final Logger log = LoggerFactory
			.getLogger(KafkaSource.class);
	ConsumerConnector consumer;
	ConsumerIterator<byte[], byte[]> it;
	String topic;
	Integer batchSize;

	public static void main(String[] args) {

	}

	public Status process() throws EventDeliveryException {
		ArrayList<Event> eventList = new ArrayList<Event>();
		byte[] message;
		Event event;
		Map<String, String> headers;
		try {
			for (int i = 0; i < batchSize; i++) {
				if (it.hasNext()) {
					message = it.next().message();
					event = new SimpleEvent();
					headers = new HashMap<String, String>();
					headers.put("timestamp",
							String.valueOf(System.currentTimeMillis()));
					log.debug("Message[" + i + "]" + ": " + new String(message));
					event.setBody(message);
					event.setHeaders(headers);
					eventList.add(event);
				}
			}
			getChannelProcessor().processEventBatch(eventList);
			return Status.READY;
		} catch (Exception e) {
			log.error("KafkaSource EXCEPTION" + e);
			return Status.BACKOFF;
		} finally {
		}
	}

	public void configure(Context context) {
		this.topic = KafkaUtil.getKafkaConfigParameter(context, "topic");
		this.batchSize = Integer.parseInt(KafkaUtil.getKafkaConfigParameter(
				context, "batch.size"));
		try {
			this.consumer = KafkaUtil.getConsumer(context);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(topic, new Integer(1));
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicCountMap);
		KafkaStream<byte[], byte[]> stream = consumerMap.get(topic).get(0);
		it = stream.iterator();
	}

	@Override
	public synchronized void stop() {
		consumer.shutdown();
		super.stop();
	}

}
