#include <ESP8266WiFi.h> 
#include <PubSubClient.h> 
#include "DHT.h" 
 
// 1. THÔNG TIN SINH VIÊN (Username & Password cho MQTT Broker)
const char* HO_TEN = "Pham Ngoc Quynh";      // Username MQTT
const char* MA_SV  = "B23DCCN704";         // Password MQTT
 
// 2. THÔNG TIN WI-FI  
const char* ssid = "quun";       // Tên Wi-Fi 
const char* password = "12345678";       // Mật khẩu Wi-Fi 
 
// 3. THÔNG TIN MOSQUITTO MQTT BROKER 
const char* mqtt_server = "192.168.2.200";     // Địa chỉ IP IPv4 của Laptop/Server
const int mqtt_port = 1880; 
 
// 4. KHAI BÁO CẢM BIẾN & LED 
#define DHTPIN D4        
#define DHTTYPE DHT11    
#define LIGHT_PIN A0     
 
#define LED1_PIN D1      
#define LED2_PIN D2      
#define LED3_PIN D3      
 
DHT dht(DHTPIN, DHTTYPE); 
WiFiClient espClient; 
PubSubClient client(espClient); 
 
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
 
void callback(char* topic, byte* payload, unsigned int length) { 
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
      client.publish("esp8266/status", "ALL LEDs are ON Successfully"); 
    } else if (message == "OFF") { 
      digitalWrite(LED1_PIN, LOW); 
      digitalWrite(LED2_PIN, LOW); 
      digitalWrite(LED3_PIN, LOW); 
      client.publish("esp8266/status", "ALL LEDs are OFF Successfully"); 
    } 
  } 
  // ----------------------------------------------------
  // 2. XỬ LÝ BẬT / TẮT TỪNG ĐÈN RIÊNG LẺ (KÈM ALIAS FAN, LIGHT, AC)
  // ----------------------------------------------------
  else if (topicStr == "esp8266/led1" || topicStr == "esp8266/fan") { 
    if (message == "ON") { 
      digitalWrite(LED1_PIN, HIGH); 
      client.publish("esp8266/status", "LED1 is ON Successfully"); 
    } else if (message == "OFF") { 
      digitalWrite(LED1_PIN, LOW); 
      client.publish("esp8266/status", "LED1 is OFF Successfully"); 
    } 
  } 
  else if (topicStr == "esp8266/led2" || topicStr == "esp8266/light") { 
    if (message == "ON") { 
      digitalWrite(LED2_PIN, HIGH); 
      client.publish("esp8266/status", "LED2 is ON Successfully"); 
    } else if (message == "OFF") { 
      digitalWrite(LED2_PIN, LOW); 
      client.publish("esp8266/status", "LED2 is OFF Successfully"); 
    } 
  } 
  else if (topicStr == "esp8266/led3" || topicStr == "esp8266/ac") { 
    if (message == "ON") { 
      digitalWrite(LED3_PIN, HIGH); 
      client.publish("esp8266/status", "LED3 is ON Successfully"); 
    } else if (message == "OFF") { 
      digitalWrite(LED3_PIN, LOW); 
      client.publish("esp8266/status", "LED3 is OFF Successfully"); 
    } 
  } 
} 
 
void reconnect() { 
  while (!client.connected()) { 
    Serial.print("Dang ket noi Mosquitto MQTT vơi TK/MK..."); 
    
    if (client.connect("ESP8266_Client", HO_TEN, MA_SV, "esp8266/status", 0, false, "ESP_OFFLINE")) { 
      Serial.println(" Thanh cong!"); 
      
      // Đảm bảo khi ESP vừa kết nối lại/reset, các đèn đều tắt và báo cho backend biết
      digitalWrite(LED1_PIN, LOW); 
      digitalWrite(LED2_PIN, LOW); 
      digitalWrite(LED3_PIN, LOW); 
      client.publish("esp8266/status", "ESP_RESET_ALL_LEDS_OFF"); 

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
  digitalWrite(LED1_PIN, LOW); 
  digitalWrite(LED2_PIN, LOW); 
  digitalWrite(LED3_PIN, LOW); 
 
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