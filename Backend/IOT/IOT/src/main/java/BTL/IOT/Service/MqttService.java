package BTL.IOT.Service;

import BTL.IOT.Entity.Action;
import BTL.IOT.Entity.DataSensor;
import BTL.IOT.Repository.ActionRepository;
import BTL.IOT.Repository.DataSensorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MqttService implements MqttCallbackExtended {

    private final DataSensorRepository dataSensorRepository;
    private final ActionRepository actionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

    @Value("${mqtt.broker-url:tcp://localhost:1880}")
    private String brokerUrl;

    @Value("${mqtt.client-id:SpringBoot_Backend}")
    private String clientId;

    @Value("${mqtt.username:Pham Ngoc Quynh}")
    private String username;

    @Value("${mqtt.password:B23DCCN704}")
    private String password;

    @Value("${mqtt.topics.sensor:esp8266/data_sensor}")
    private String sensorTopic;

    @Value("${mqtt.topics.status:esp8266/status}")
    private String statusTopic;

    private MqttClient mqttClient;
    private boolean connected = false;

    private volatile long lastHeartbeat = 0L;
    private final Map<String, CompletableFuture<String>> pendingAcks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        connect();
    }

    public synchronized void connect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                connected = true;
                return;
            }

            mqttClient = new MqttClient(brokerUrl, clientId + "_" + System.currentTimeMillis(), new MemoryPersistence());
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            log.info("Connecting to MQTT Broker at {}", brokerUrl);
            mqttClient.connect(options);
            connected = true;
            log.info("Successfully connected to MQTT Broker");

            subscribeTopics();
        } catch (Exception e) {
            connected = false;
            log.warn("Could not connect to MQTT broker ({}) - will retry later: {}", brokerUrl, e.getMessage());
        }
    }

    private void subscribeTopics() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(sensorTopic, 0);
                mqttClient.subscribe(statusTopic, 0);
                log.info("Subscribed to MQTT topics: {}, {}", sensorTopic, statusTopic);
            }
        } catch (Exception e) {
            log.error("Failed to subscribe to MQTT topics: {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public boolean isEspOnline() {
        return isConnected() && (System.currentTimeMillis() - lastHeartbeat < 8000);
    }

    public CompletableFuture<String> registerAck(String key) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingAcks.put(key.toLowerCase(), future);
        return future;
    }

    public void removeAck(String key) {
        pendingAcks.remove(key.toLowerCase());
    }

    private void completeAck(String key, String payload) {
        CompletableFuture<String> future = pendingAcks.get(key.toLowerCase());
        if (future != null) {
            future.complete(payload);
        }
    }

    public void publish(String topic, String message) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                connect();
            }
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(0);
                mqttClient.publish(topic, mqttMessage);
                log.info("Published to MQTT [{}]: {}", topic, message);
            } else {
                log.warn("Cannot publish to MQTT, client not connected. Topic: {}, msg: {}", topic, message);
            }
        } catch (Exception e) {
            log.error("Error publishing to MQTT topic {}: {}", topic, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        connected = false;
        log.warn("MQTT connection lost: {}", cause != null ? cause.getMessage() : "Unknown reason");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        lastHeartbeat = System.currentTimeMillis();
        log.info("MQTT Message arrived [{}] : {}", topic, payload);

        if (sensorTopic.equals(topic)) {
            handleSensorPayload(payload);
        } else if (statusTopic.equals(topic)) {
            handleStatusPayload(payload);
        }
    }

    private void handleStatusPayload(String payload) {
        log.info("Device status feedback from ESP: {}", payload);

        // Trường hợp ESP vừa reset hoặc báo offline
        if (payload.contains("RESET") || payload.contains("ALL_LEDS_OFF") || payload.contains("ESP_OFFLINE")) {
            resetAllDevicesToOff();
        }

        String upper = payload.toUpperCase();
        if (upper.contains("LED1")) {
            completeAck("led1", payload);
        }
        if (upper.contains("LED2")) {
            completeAck("led2", payload);
        }
        if (upper.contains("LED3")) {
            completeAck("led3", payload);
        }
    }

    public void resetAllDevicesToOff() {
        LocalDateTime now = LocalDateTime.now();
        for (int id = 1; id <= 3; id++) {
            actionRepository.save(Action.builder()
                    .deviceId(id)
                    .action("OFF")
                    .status("OFF")
                    .time(now)
                    .build());
        }
        log.info("Reset all devices in DB to OFF due to ESP reset/offline.");
    }

    private void handleSensorPayload(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            LocalDateTime now = LocalDateTime.now();

            // Sensor 1: Temperature
            if (root.has("temperature")) {
                double temp = root.get("temperature").asDouble();
                dataSensorRepository.save(DataSensor.builder()
                        .sensorId(1)
                        .value(BigDecimal.valueOf(temp))
                        .time(now)
                        .build());
            }

            // Sensor 2: Humidity
            if (root.has("humidity")) {
                double hum = root.get("humidity").asDouble();
                dataSensorRepository.save(DataSensor.builder()
                        .sensorId(2)
                        .value(BigDecimal.valueOf(hum))
                        .time(now)
                        .build());
            }

            // Sensor 3: Light
            if (root.has("light")) {
                double light = root.get("light").asDouble();
                dataSensorRepository.save(DataSensor.builder()
                        .sensorId(3)
                        .value(BigDecimal.valueOf(light))
                        .time(now)
                        .build());
            }

            log.info("Saved sensor readings into database at {}", now);
        } catch (Exception e) {
            log.error("Error parsing sensor payload [{}]: {}", payload, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Delivery complete
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        connected = true;
        log.info("MQTT connect complete (reconnect={}) to {}", reconnect, serverURI);
        subscribeTopics();
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (Exception e) {
            log.warn("Error disconnecting MQTT client: {}", e.getMessage());
        }
    }
}
