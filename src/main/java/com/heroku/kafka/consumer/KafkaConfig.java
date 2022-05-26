package com.heroku.kafka.consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.lang.System.getenv;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;

import com.github.jkutner.EnvKeyStore;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

@EnableKafka
@Configuration
public class KafkaConfig {

  	private String topic;

	public Map<String, Object> getProperties() {
		return buildDefaults();
	}

	private Map<String, Object> buildDefaults() {
		Map<String, Object> properties = new HashMap<>();
		List<String> hostPorts = Lists.newArrayList();

		for (String url : Splitter.on(",").split(checkNotNull(getenv("KAFKA_URL")))) {
			try {
				URI uri = new URI(url);
				hostPorts.add(format("%s:%d", uri.getHost(), uri.getPort()));

				switch (uri.getScheme()) {
				case "kafka":
					properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
					break;
				case "kafka+ssl":
					properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

					try {
						EnvKeyStore envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT");
						EnvKeyStore envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY",
								"KAFKA_CLIENT_CERT");

						File trustStore = envTrustStore.storeTemp();
						File keyStore = envKeyStore.storeTemp();

						properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, envTrustStore.type());
						properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStore.getAbsolutePath());
						properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, envTrustStore.password());
						properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, envKeyStore.type());
						properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStore.getAbsolutePath());
						properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, envKeyStore.password());
					} catch (Exception e) {
						throw new RuntimeException("There was a problem creating the Kafka key stores", e);
					}
					break;
				default:
					throw new IllegalArgumentException(format("unknown scheme; %s", uri.getScheme()));
				}
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

        properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
		properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, Joiner.on(",").join(hostPorts));
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Joiner.on(",").join(hostPorts));
		properties.put(ConsumerConfig.GROUP_ID_CONFIG,
	                "group_id");
		properties.put(
	         ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
	         StringDeserializer.class);
		properties.put(
	         ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
	         StringDeserializer.class);
		return properties;
	}

	@Bean
	public KafkaAdmin kafkaAdmin() {
		return new KafkaAdmin(buildDefaults());
	}
	
	@Bean
	public ConsumerFactory<String, Object> consumerFactory() {
		
		Map<String, Object> defaults = buildDefaults();
		return new DefaultKafkaConsumerFactory<>(defaults);
	}

	public ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}

	public String getTopic() {
		return topic;
	}
}
