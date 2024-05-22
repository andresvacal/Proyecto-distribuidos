package javeriana.edu;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class SprinklerActuator {
    // Method to activate the sprinkler
    public void activate() {
        System.out.println("Sprinkler activated!");
    }

    // Main method to run as a standalone listener
    public static void main(String[] args) {
        SprinklerActuator sprinkler = new SprinklerActuator();

        // Set up ZeroMQ Context and Subscriber Socket
        try (ZContext context = new ZContext()) {
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5556"); // Assuming the publisher (sensor) is publishing on this port
            subscriber.subscribe("ACTIVATE_SPRINKLER".getBytes(ZMQ.CHARSET)); // Subscribe to activation messages

            System.out.println("Esperando a comando de activación...");

            while (!Thread.currentThread().isInterrupted()) {
                // Receive messages
                String command = subscriber.recvStr(0);
                if ("ACTIVATE_SPRINKLER".equals(command)) {
                    sprinkler.activate();
                }
            }
        }
    }
}
