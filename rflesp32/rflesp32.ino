#include <WiFi.h>
#include <Wire.h>
#include "MAX30105.h"
#include <Adafruit_MLX90614.h>
#include <ESPmDNS.h>
#include <WebServer.h>

#define ECG_PIN 34  // AD8232 ECG sensor connected to GPIO34
#define SDA_PIN 21   // I2C SDA
#define SCL_PIN 22   // I2C SCL

const char* ssid = "So-wifi";
const char* password = "12345678";

MAX30105 particleSensor;
Adafruit_MLX90614 mlx = Adafruit_MLX90614();
WebServer server(80);

void setup() {
    Serial.begin(115200);
    Serial.println("Initializing ESP32 with Sensors...");
    
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nConnected to WiFi");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());

    if (MDNS.begin("esp32")) {
        Serial.println("MDNS responder started");
    }

    Wire.begin(SDA_PIN, SCL_PIN);
    
    if (!particleSensor.begin(Wire)) {
        Serial.println("MAX30102 not found!");
        while (1);
    }
    particleSensor.setup();

    if (!mlx.begin()) {
        Serial.println("MLX90614 not found!");
        while (1);
    }
    
    pinMode(ECG_PIN, INPUT);

    // Endpoint to provide sensor data in JSON format
    server.on("/sensorData", HTTP_GET, sendSensorDataAsJson);
    
    server.begin();
    Serial.println("Web server started");
}

void loop() {
    server.handleClient();
    delay(10);
}

void sendSensorDataAsJson() {
    float temperature = mlx.readObjectTempC();
    int ecgValue = analogRead(ECG_PIN);
    long irValue = particleSensor.getIR();
    
    int pulse = map(ecgValue, 0, 4095, 60, 120);
    int oxygenLevel = map(irValue, 0, 100000, 90, 100);
    
    // Ensure valid data
    if (isnan(temperature)) temperature = 0.0;
    if (isnan(pulse)) pulse = 0;
    if (isnan(oxygenLevel)) oxygenLevel = 0;
    
    // Construct JSON
    String json = "{";
    json += "\"temperature\":" + String(temperature, 2) + ",";
    json += "\"oxygen\":" + String(oxygenLevel) + ",";
    json += "\"pulse\":" + String(pulse);
    json += "}";

    Serial.println("Sending JSON: " + json); // Debugging

    server.sendHeader("Connection", "close"); // Force ESP32 to close properly
    server.send(200, "application/json", json);
}
