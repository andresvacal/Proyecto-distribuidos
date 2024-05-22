package javeriana.edu;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ProxyServerBackUp {
    private static final String CLOUD_API_URL = "http://localhost:8080/api/sensors/data";
    private static HttpClient httpClient = HttpClients.createDefault();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<Integer, List<Double>> dailyHumidity = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        setupPeriodicTasks();
        try (ZContext context = new ZContext()) {
            ZMQ.Socket pullSocket = context.createSocket(SocketType.PULL);
            ZMQ.Socket repSocket = context.createSocket(SocketType.REP);  // Changed to REP
            pullSocket.bind("tcp://*:5559");
            repSocket.bind("tcp://*:5560");  // Bind REP socket to handle replies

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            System.out.println("Proxy Server is active and listening to sensors... \n");

            while (!Thread.currentThread().isInterrupted()) {
                byte[] messageBytes = pullSocket.recv(0);
                String message = new String(messageBytes, ZMQ.CHARSET);
                String timestamp = sdf.format(new Date());
                System.out.println(timestamp + " - Received: " + message);

                if ("PING".equals(message)) {
                    repSocket.send("PONG".getBytes(ZMQ.CHARSET), 0);
                    System.out.println(timestamp + " - Sent: PONG");
                    continue; // Skip further processing for PING messages
                }

                // Process other sensor data types
                if (message.contains("Temperatura")) {
                    handleTemperature(message, timestamp);
                } else if (message.contains("Humedad")) {
                    handleHumidity(message, timestamp);
                } else if (message.contains("Humo")) {
                    handleSmoke(message, timestamp);
                } else {
                    System.out.println(timestamp + " - Unknown sensor data type: " + message);
                }
            }
        }
    }

    private static void setupPeriodicTasks() {
        scheduler.scheduleAtFixedRate(() -> calculateAverageHumidity(), 0, 5, TimeUnit.SECONDS);
    }

    private static void calculateAverageHumidity() {
        dailyHumidity.forEach((day, values) -> {
            double average = values.stream().mapToDouble(d -> d).average().orElse(0.0);
            if (average < 20) { // Define your threshold
                System.out.println("Alert: Low average humidity detected for day " + day);
            }
            values.clear(); // Clear after processing to start new average calculation
        });
    }

    private static void handleTemperature(String message, String timestamp) {
        System.out.println("Processing temperature data: " + message);
        sendToCloud("Temperature", message, timestamp);
    }

    private static void handleHumidity(String message, String timestamp) {
        System.out.println("Processing humidity data: " + message);
        double humidityValue = extractHumidityValue(message); // Implement this method based on your data format
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        dailyHumidity.computeIfAbsent(dayOfYear, k -> new CopyOnWriteArrayList<>()).add(humidityValue);
        sendToCloud("Humidity", message, timestamp);
    }

    private static double extractHumidityValue(String message) {
        // Extraction logic here
        return Double.parseDouble(message.split(":")[1].trim()); // Simplistic example, adjust accordingly
    }

    private static void handleSmoke(String message, String timestamp) {
        System.out.println("Processing smoke data: " + message);
        sendToCloud("Smoke", message, timestamp);
    }

    private static void sendToCloud(String sensorType, String data, String timestamp) {
        HttpPost post = new HttpPost(CLOUD_API_URL);
        try {
            String json = String.format("{\"sensorType\":\"%s\", \"value\":\"%s\", \"timestamp\":\"%s\"}", sensorType, data, timestamp);
            StringEntity entity = new StringEntity(json);
            post.setEntity(entity);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Response Status Code: " + statusCode);

            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseBody = EntityUtils.toString(responseEntity);
                System.out.println("Response Body: " + responseBody);
            }

            System.out.println("Data sent to cloud: " + json);
        } catch (IOException e) {
            System.err.println("Error sending data to the cloud: " + e.getMessage());
        }
    }
}