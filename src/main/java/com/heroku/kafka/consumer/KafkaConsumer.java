package com.heroku.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

	@KafkaListener(topics ="#{systemEnvironment['KAFKA_TOPIC']}",
            containerFactory = "concurrentKafkaListenerContainerFactory")
	public void
	consume(String message)
	{
		System.out.println("message = " + message);
	}
}
