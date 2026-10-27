#include "DHT.h"
#include <EEPROM.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>

// 1. THÔNG TIN SINH VIÊN (Username & Password cho MQTT Broker)
const char *HO_TEN = "Pham Ngoc Quynh"; // Username MQTT
const char *MA_SV = "B23DCCN704";       // Password MQTT

// 2. THÔNG TIN WI-FI
const char *ssid = "quun";         // Tên Wi-Fi
const char *password = "12345678"; // Mật khẩu Wi-Fi

// 3. THÔNG TIN MOSQUITTO MQTT BROKER
const char *mqtt_server = "192.168.19.200"; // Địa chỉ IP IPv4 của Laptop/Server
const int mqtt_port = 1880;

// 4. KHAI BÁO CẢM BIẾN & LED
#define DHTPIN D4
#define DHTTYPE DHT11
#define LIGHT_PIN A0

#define LED1_PIN D1
#define LED2_PIN D2
#define LED3_PIN D3

// Địa chỉ lưu trạng thái LED trong bộ nhớ EEPROM (không mất khi cúp điện /
// reset)
#define EEPROM_MAGIC 0xA5
#define EEPROM_ADDR_MAGIC 0
#define EEPROM_ADDR_LED1 1
#define EEPROM_ADDR_LED2 2
#define EEPROM_ADDR_LED3 3

DHT dht(DHTPIN, DHTTYPE);
WiFiClient espClient;
PubSubClient client(espClient);

void init_eeprom() {
  EEPROM.begin(512);
  if (EEPROM.read(EEPROM_ADDR_MAGIC) != EEPROM_MAGIC) {
    EEPROM.write(EEPROM_ADDR_MAGIC, EEPROM_MAGIC);
    EEPROM.write(EEPROM_ADDR_LED1, 0);
    EEPROM.write(EEPROM_ADDR_LED2, 0);
    EEPROM.write(EEPROM_ADDR_LED3, 0);
    EEPROM.commit();
  }
}

void restore_led_states() {
  int s1 = EEPROM.read(EEPROM_ADDR_LED1);
  int s2 = EEPROM.read(EEPROM_ADDR_LED2);
  int s3 = EEPROM.read(EEPROM_ADDR_LED3);

  digitalWrite(LED1_PIN, s1 ? HIGH : LOW);
  digitalWrite(LED2_PIN, s2 ? HIGH : LOW);
  digitalWrite(LED3_PIN, s3 ? HIGH : LOW);
  Serial.println("Da khoi phuc trang thai den tu EEPROM: LED1=" + String(s1) +
                 ", LED2=" + String(s2) + ", LED3=" + String(s3));
}

void save_led_state(int ledNum, int state) {
  int addr = (ledNum == 1)
                 ? EEPROM_ADDR_LED1
                 : (ledNum == 2 ? EEPROM_ADDR_LED2 : EEPROM_ADDR_LED3);
  EEPROM.write(addr, state ? 1 : 0);
  EEPROM.commit();
}

void setup_wifi() {
  delay(10);
  Serial.print("Dang ket noi Wi-Fi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWi-Fi da ket noi! IP ESP: " + WiFi.localIP().toString());
}

void callback(char *topic, byte *payload, unsigned int length) {
  String message = "";
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  String topicStr = String(topic);
  Serial.println("Nhan lenh [" + topicStr + "]: " + message);

  // ----------------------------------------------------
  // 1. XỬ LÝ BẬT / TẮT TẤT CẢ ĐÈN CÙNG LÚC
  // ----------------------------------------------------
  if (topicStr == "esp8266/led_all") {
    if (message == "ON") {
      digitalWrite(LED1_PIN, HIGH);
      digitalWrite(LED2_PIN, HIGH);
      digitalWrite(LED3_PIN, HIGH);
      save_led_state(1, 1);
      save_led_state(2, 1);
      save_led_state(3, 1);
      client.publish("esp8266/status", "ALL LEDs are ON Successfully");
    } else if (message == "OFF") {
      digitalWrite(LED1_PIN, LOW);
      digitalWrite(LED2_PIN, LOW);
      digitalWrite(LED3_PIN, LOW);
      save_led_state(1, 0);
      save_led_state(2, 0);
      save_led_state(3, 0);
      client.publish("esp8266/status", "ALL LEDs are OFF Successfully");
    }
  }
  // ----------------------------------------------------
  // 2. XỬ LÝ BẬT / TẮT TỪNG ĐÈN RIÊNG LẺ (KÈM ALIAS FAN, LIGHT, AC)
  // ----------------------------------------------------
  else if (topicStr == "esp8266/led1" || topicStr == "esp8266/fan") {
    if (message == "ON") {
      digitalWrite(LED1_PIN, HIGH);
      save_led_state(1, 1);
      client.publish("esp8266/status", "LED1 is ON Successfully");
    } else if (message == "OFF") {
      digitalWrite(LED1_PIN, LOW);
      save_led_state(1, 0);
      client.publish("esp8266/status", "LED1 is OFF Successfully");
    }
  } else if (topicStr == "esp8266/led2" || topicStr == "esp8266/light") {
    if (message == "ON") {
      digitalWrite(LED2_PIN, HIGH);
      save_led_state(2, 1);
      client.publish("esp8266/status", "LED2 is ON Successfully");
    } else if (message == "OFF") {
      digitalWrite(LED2_PIN, LOW);
      save_led_state(2, 0);
      client.publish("esp8266/status", "LED2 is OFF Successfully");
    }
  } else if (topicStr == "esp8266/led3" || topicStr == "esp8266/ac") {
    if (message == "ON") {
      digitalWrite(LED3_PIN, HIGH);
      save_led_state(3, 1);
      client.publish("esp8266/status", "LED3 is ON Successfully");
    } else if (message == "OFF") {
      digitalWrite(LED3_PIN, LOW);
      save_led_state(3, 0);
      client.publish("esp8266/status", "LED3 is OFF Successfully");
    }
  }
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Dang ket noi Mosquitto MQTT vơi TK/MK...");

    if (client.connect("ESP8266_Client", HO_TEN, MA_SV, "esp8266/status", 0,
                       false, "ESP_OFFLINE")) {
      Serial.println(" Thanh cong!");

      // Báo cho backend biết ESP vừa online và trạng thái hiện tại
      int s1 = EEPROM.read(EEPROM_ADDR_LED1);
      int s2 = EEPROM.read(EEPROM_ADDR_LED2);
      int s3 = EEPROM.read(EEPROM_ADDR_LED3);
      String onlineMsg = "ESP_ONLINE:led1=" + String(s1 ? "ON" : "OFF") +
                         ",led2=" + String(s2 ? "ON" : "OFF") +
                         ",led3=" + String(s3 ? "ON" : "OFF");
      client.publish("esp8266/status", onlineMsg.c_str());

      client.subscribe("esp8266/led1");
      client.subscribe("esp8266/led2");
      client.subscribe("esp8266/led3");
      client.subscribe("esp8266/led_all");
      client.subscribe("esp8266/fan");
      client.subscribe("esp8266/light");
      client.subscribe("esp8266/ac");
    } else {
      Serial.print(" Loi, rc=");
      Serial.print(client.state());
      Serial.println(" Thu lai sau 5 giay...");
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(LED1_PIN, OUTPUT);
  pinMode(LED2_PIN, OUTPUT);
  pinMode(LED3_PIN, OUTPUT);

  // Khởi tạo EEPROM và khôi phục trạng thái đèn cũ ngay khi bật nguồn
  init_eeprom();
  restore_led_states();

  dht.begin();
  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

unsigned long lastMsg = 0;
void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  unsigned long now = millis();
  if (now - lastMsg > 3000) {
    lastMsg = now;

    float temp = dht.readTemperature();
    float hum = dht.readHumidity();
    int lightVal = analogRead(LIGHT_PIN);

    if (!isnan(temp) && !isnan(hum)) {
      String payload = "{";
      payload += "\"temperature\":" + String(temp) + ",";
      payload += "\"humidity\":" + String(hum) + ",";
      payload += "\"light\":" + String(lightVal);
      payload += "}";

      // GỬI DỮ LIỆU CẢM BIẾN LÊN TOPIC RIÊNG: esp8266/data_sensor
      client.publish("esp8266/data_sensor", payload.c_str());
      Serial.println("Sent JSON to data_sensor -> " + payload);
    } else {
      Serial.println("Loi: Khong doc duoc DHT11!");
    }
  }
}