package javeriana.edu;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class SensorHumedad
{
    private static final String PRIMARY_PROXY = "tcp://localhost:5555";
    private static final String BACKUP_PROXY = "tcp://localhost:5559";
    public static void main(String[] args)
    {
        // Archivo de configuración
        String archivoConfiguracion = "C:\\Users\\estudiante\\Desktop\\Proyecto---Sistemas-Distribuidos\\Sensores\\src\\main\\java\\javeriana\\edu\\configHumedad.txt";


        // Crear socket PUSH para enviar datos
        try (ZContext context = new ZContext())
        {
            ZMQ.Socket pushSocket = context.createSocket(SocketType.PUSH);

            // Para pruebas locales:
            pushSocket.connect("tcp://localhost:5555");
            boolean connected = connectToProxy(pushSocket, PRIMARY_PROXY);
            if (!connected) {
                connected = connectToProxy(pushSocket, BACKUP_PROXY);
            }
            if (!connected) {
                System.err.println("Error: Unable to connect to any proxy server.");
                return;
            }

            pushSocket.connect("tcp://10.43.103.116:5555");

            double probabilidadCorrectos = 0.0;
            double probabilidadFueraDeRango = 0.0;
            double probabilidadErrores = 0.0;
            double totalProbabilidades;


            // Leer archivo de configuración
            try (BufferedReader br = new BufferedReader(new FileReader(archivoConfiguracion)))
            {
                String linea;
                while ((linea = br.readLine()) != null)
                {
                    if (linea.startsWith("probabilidadCorrectos"))
                    {
                        probabilidadCorrectos = Double.parseDouble(linea.split("=")[1]);
                    }

                    else if (linea.startsWith("probabilidadFueraDeRango"))
                    {
                        probabilidadFueraDeRango = Double.parseDouble(linea.split("=")[1]);
                    }

                    else if (linea.startsWith("probabilidadErrores"))
                    {
                        probabilidadErrores = Double.parseDouble(linea.split("=")[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }


            // Validar que la suma de las probabilidades sea 1
            totalProbabilidades = probabilidadCorrectos + probabilidadFueraDeRango + probabilidadErrores;
            totalProbabilidades = Math.round(totalProbabilidades * 10) / 10.0;

            System.out.println("Total de probabilidades: " + totalProbabilidades);

            if (totalProbabilidades != 1.0) {
                System.out.println("Error en la configuración: La suma de las probabilidades no es 1.");
                return;
            }



            Random random = new Random();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (!Thread.currentThread().isInterrupted())
            {
                double r = random.nextDouble();
                int valor;

                if (r < probabilidadCorrectos)
                {
                    valor = 70 + random.nextInt(31); // 70% a 100%
                }

                else if (r < probabilidadCorrectos + probabilidadFueraDeRango)
                {
                    valor = random.nextBoolean() ? 65 - random.nextInt(20) : 101 + random.nextInt(20); // Fuera de rango
                }

                else
                {
                    valor = -random.nextInt(100); // Error
                }


                // Enviar mensaje al servidor
                LocalDateTime ahora = LocalDateTime.now();
                String mensaje = String.format("Humedad %d%% a las %s", valor, dtf.format(ahora));
                System.out.println("Enviando: " + mensaje);
                pushSocket.send(mensaje.getBytes(ZMQ.CHARSET), 0);

                try
                {
                    Thread.sleep(4000);
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
