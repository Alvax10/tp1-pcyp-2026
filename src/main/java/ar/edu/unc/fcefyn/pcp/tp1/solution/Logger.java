package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public final class Logger {
    private static BufferedWriter EventsWriter;
    private static BufferedWriter ElementsWriter;

    public static void init() {

        // CREA DOS ARCHIVOS DE TEXTO
        // EVENTOS Y ELEMENTOS
        try {
            EventsWriter = new BufferedWriter(
                    new FileWriter("resultados/eventos.csv", true));

            ElementsWriter = new BufferedWriter(
                    new FileWriter("resultados/elementos.csv", true));

        } catch (IOException e){
            System.err.println("No se pudo crear los writters");
        }
    }

    private static void write(BufferedWriter writter, String valueToWrite) {
        if (writter == null) {
            return;
        }
        try {
            writter.write(valueToWrite);
            writter.newLine();
            writter.flush();
        } catch (IOException e) {
            System.err.println("No se pudo escribir en el archivo");
        }
    }

    public static void recordEvent(int sequence, long elapsedMs,Thread thread, int orderId, StageState stage,
                              EventState event, OrderState from, OrderState to, String printerId) {

        String line = String.join(";",
                Long.toString(++sequence),
                Long.toString(elapsedMs),
                Thread.currentThread().getName(),
                Integer.toString(orderId),
                stage.name(),
                event.toString(),
                from.toString() == null ? "" : from.toString(),
                to.toString() == null ? "" : to.toString(),
                printerId == null ? "" : printerId);

        write(EventsWriter, line);
    }

    public static void recordEvent(int orderId, OrderState finalState, String printerId, int assignmentCount,
                                   int validationCount, int printingCount, int qualityControlCount) {

        String line = String.join(";",
                Integer.toString(orderId),
                finalState.toString(),
                printerId,
                Integer.toString(assignmentCount),
                Integer.toString(validationCount),
                Integer.toString(printingCount),
                Integer.toString(qualityControlCount),
                Thread.currentThread().getName());

        write(ElementsWriter, line);
    }
}