package javeriana.edu;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class SensorHumo {
    private static final String PRIMARY_PROXY = "tcp://localhost:5555";
    private static final String BACKUP_PROXY = "tcp://localhost:5559";
    public static void main(String[] args) {
        // Archivo de configuración
        String archivoConfiguracion = "C:\\Users\\estudiante\\Desktop\\Proyecto---Sistemas-Distribuidos\\Sensores\\src\\main\\java\\javeriana\\edu\\configHumo.txt";

        try (ZContext context = new ZContext()) {
            // Create PUSH socket for sending data
            ZMQ.Socket pushSocket = context.createSocket(SocketType.PUSH);
            boolean connected = connectToProxy(pushSocket, PRIMARY_PROXY);
            if (!connected) {
                connected = connectToProxy(pushSocket, BACKUP_PROXY);
            }
            if (!connected) {
                System.err.println("Error: Unable to connect to any proxy server.");
                return;
            }
            pushSocket.connect("tcp://localhost:5555"); // Connect to the proxy for data

            // Create PUB socket for publishing sprinkler commands
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5556"); // Bind to a port to publish sprinkler commands

            double probabilidadNoHumo = 0.0;
            double probabilidadHumo = 0.0;
            double probabilidadErrores = 0.0;

            // Read configuration from file
            try (BufferedReader br = new BufferedReader(new FileReader(archivoConfiguracion))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.startsWith("probabilidadNoHumo")) {
                        probabilidadNoHumo = Double.parseDouble(linea.split("=")[1]);
                    } else if (linea.startsWith("probabilidadHumo")) {
                        probabilidadHumo = Double.parseDouble(linea.split("=")[1]);
                    } else if (linea.startsWith("probabilidadErrores")) {
                        probabilidadErrores = Double.parseDouble(linea.split("=")[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Check if the total probabilities are 1
            double totalProbabilidades = probabilidadNoHumo + probabilidadHumo + probabilidadErrores;
            if (totalProbabilidades != 1.0) {
                System.out.println("Error in configuration: The sum of probabilities is not 1.");
                return;
            }

            Random random = new Random();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (!Thread.currentThread().isInterrupted()) {
                double r = random.nextDouble();
                String valor;
                if (r < probabilidadNoHumo) {
                    valor = "No Humo";
                } else if (r < probabilidadNoHumo + probabilidadHumo) {
                    valor = "Humo";
                    // Smoke detected, publish activation command for the sprinkler
                    publisher.send("ACTIVATE_SPRINKLER".getBytes(ZMQ.CHARSET));
                } else {
                    valor = "Error";
                }

                // Send data message to the Proxy Server
                LocalDateTime ahora = LocalDateTime.now();
                String mensaje = String.format("Humo: %s a las %s", valor, dtf.format(ahora));
                System.out.println("Sending: " + mensaje);
                pushSocket.send(mensaje.getBytes(ZMQ.CHARSET), 0);

                try {
                    Thread.sleep(4000); // Simulation timing adjustment
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    private static boolean connectToProxy(ZMQ.Socket socket, String address) {
        try {
            socket.connect(address);
            return true;
        } catch (Exception e) {
            System.out.println("Failed to connect to " + address);
            return false;
        }
    }
}
