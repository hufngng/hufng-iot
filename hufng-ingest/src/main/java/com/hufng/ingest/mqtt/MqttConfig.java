package com.hufng.ingest.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Shared subscription ("$share/hufng-ingest/...") load-balances the topic across every
 * instance of this service in the group — each message goes to exactly one instance,
 * the MQTT equivalent of a Kafka consumer group. QoS 1 matches the device's own
 * at-least-once retry behaviour (dedupe happens downstream via device-supplied seq).
 *
 * Gated by mqtt.enabled so `./gradlew test` never needs a real broker.
 */
@Configuration
@ConditionalOnProperty(name = "mqtt.enabled", matchIfMissing = true)
public class MqttConfig {

    private static final String SHARED_TOPIC = "$share/hufng-ingest/device/+/telemetry";

    @Bean
    public MqttPahoClientFactory mqttClientFactory(
            org.springframework.core.env.Environment env) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{env.getProperty("mqtt.broker-url", "tcp://localhost:1883")});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(MqttPahoClientFactory mqttClientFactory,
                                                                   MessageChannel mqttInputChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                "hufng-ingest-" + java.util.UUID.randomUUID(), mqttClientFactory, SHARED_TOPIC);
        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler telemetryMessageHandler(TelemetryIngestHandler telemetryIngestHandler) {
        return telemetryIngestHandler::handle;
    }
}
