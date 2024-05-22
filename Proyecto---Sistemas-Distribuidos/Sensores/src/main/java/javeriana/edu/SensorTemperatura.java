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

public class SensorTemperatura
{
    public static void main(String[] args)
    {
        // Archivo de configuración
        String archivoConfiguracion = "C:\\Users\\estudiante\\Desktop\\Proyecto---Sistemas-Distribuidos\\Sensores\\src\\main\\java\\javeriana\\edu\\configTemperatura.txt";

        // Crear socket PUSH para enviar datos
        try (ZContext context = new ZContext())
        {
            ZMQ.Socket pushSocket = context.createSocket(SocketType.PUSH);
            // Para pruebas locales:
            pushSocket.connect("tcp://localhost:5555");
            // Para conectarlo con el proxy:
            // pushSocket.connect("tcp://10.43.103.116:5555");
            double probabilidadCorrectos = 0.0;
            double probabilidadFueraDeRango = 0.0;
            double probabilidadErrores = 0.0;
            double totalProbabilidades;

            try (BufferedReader br = new BufferedReader(new FileReader(archivoConfiguracion)))
            {
                String linea;
                while ((linea = br.readLine()) != null)
                {
                    if (linea.startsWith("probabilidadCorrectos"))
                    {
                        probabilidadCorrectos = Double.parseDouble(linea.split("=")[1]);
                        System.out.println("Probabilidad de Correctos: " + probabilidadCorrectos);
                    }

                    else if (linea.startsWith("probabilidadFueraDeRango"))
                    {
                        probabilidadFueraDeRango = Double.parseDouble(linea.split("=")[1]);
                        System.out.println("Probabilidad de Fuera de Rango: " + probabilidadFueraDeRango);
                    }

                    else if (linea.startsWith("probabilidadErrores"))
                    {
                        probabilidadErrores = Double.parseDouble(linea.split("=")[1]);
                        System.out.println("Probabilidad de Errores: " + probabilidadErrores + "\n");
                    }
                }
            } catch (IOException e)
            {
                e.printStackTrace();
                return;
            }

            // Redondear total de probabilidades a 1 decimal
            totalProbabilidades = probabilidadCorrectos + probabilidadFueraDeRango + probabilidadErrores;
            totalProbabilidades = Math.round(totalProbabilidades * 10) / 10.0;


            System.out.println("Total de probabilidades: " + totalProbabilidades);

            // Verificar que la suma de las probabilidades sea 1
            if (totalProbabilidades != 1.0)
            {
                System.out.println("Error en la configuración: La suma de las probabilidades no es 1.");
                return; // Detiene la ejecución del sensor
            }

            Random random = new Random();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (!Thread.currentThread().isInterrupted())
            {
                double valor;
                double r = random.nextDouble();

                if (r < probabilidadCorrectos)
                {
                    valor = 11 + (29.4 - 11) * random.nextDouble();
                }

                else if (r < probabilidadCorrectos + probabilidadFueraDeRango)
                {
                    valor = random.nextBoolean() ? 10 - random.nextDouble() * 10 : 29.5 + random.nextDouble() * 10;
                }

                else
                {
                    valor = -random.nextDouble() * 100;
                }

                LocalDateTime ahora = LocalDateTime.now();
                String mensaje = String.format("Temperatura %.1f a las %s", valor, dtf.format(ahora));
                System.out.println("Enviando: " + mensaje);
                pushSocket.send(mensaje.getBytes(ZMQ.CHARSET), 0);

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
