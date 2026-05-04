package com.capysoft.tuevento;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Profile("testcontainers")
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
				.waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1));
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
				.withExposedPorts(6379)
				.waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));
	}

}
