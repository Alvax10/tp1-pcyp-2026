package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Escribe eventos.csv y elementos.csv dentro de config.outputDirectory().
 *
 * Recurso compartido: los writers y el contador de secuencia. Todos los metodos publicos son
 * static synchronized (monitor = Logger.class), asi que dos hilos nunca escriben a la vez.
 * Ademas, recordEvent se llama DENTRO del lock de OrderSource, por lo que las filas quedan en el
 * mismo orden en que se confirman las transiciones (lo pide el enunciado).
 *
 * Ciclo de vida: init(...) antes de arrancar los hilos -> record* durante la simulacion ->
 * close() despues de los join().
 */
public final class Logger {

    public static final String EVENTS_HEADER =
            "sequence;elapsedMs;thread;orderId;stage;event;fromState;toState;printer";
    public static final String ELEMENTS_HEADER =
            "orderId;finalState;printer;assignmentCount;validationCount;printingCount;qualityControlCount";

    private static BufferedWriter eventsWriter;
    private static BufferedWriter elementsWriter;
    private static long sequence;
    private static long startMillis;

    private Logger() {
    }

    /**
     * Crea (o pisa) los archivos en outputDirectory y escribe las cabeceras.
     * startMillis es el instante de inicio de la simulacion, para calcular elapsedMs.
     */
    public static synchronized void init(Path outputDirectory, long simulationStartMillis) throws IOException {
        Files.createDirectories(outputDirectory);
        // newBufferedWriter sin opciones = CREATE + TRUNCATE_EXISTING: cada corrida empieza de cero
        eventsWriter = Files.newBufferedWriter(outputDirectory.resolve("eventos.csv"), StandardCharsets.UTF_8);
        elementsWriter = Files.newBufferedWriter(outputDirectory.resolve("elementos.csv"), StandardCharsets.UTF_8);
        sequence = 0;
        startMillis = simulationStartMillis;

        write(eventsWriter, EVENTS_HEADER);
        write(elementsWriter, ELEMENTS_HEADER);
    }

    /** Fila inicial de cada orden: stage INITIALIZATION, fromState y printer vacios. */
    public static synchronized void recordCreated(int orderId) {
        writeEvent(Thread.currentThread().getName(), orderId, StageState.INITIALIZATION,
                EventState.ORDER_CREATED, null, OrderState.CREATED, null);
    }

    /** Una fila por transicion de una orden. Se llama dentro del lock de OrderSource. */
    public static synchronized void recordEvent(Worker worker, int orderId, OrderState from,
                                                OrderState to, String printerId) {
        writeEvent(Thread.currentThread().getName(), orderId, worker.getStage(),
                EventState.ORDER_STATE_CHANGED, from, to, printerId);
    }

    /** Una fila por orden. Se debe llamar al final, recorriendo las ordenes por orderId. */
    public static synchronized void recordElement(int orderId, OrderState finalState, String printerId,
                                                  int assignmentCount, int validationCount,
                                                  int printingCount, int qualityControlCount) {
        String line = String.join(";",
                Integer.toString(orderId),
                finalState.name(),
                nullToEmpty(printerId),
                Integer.toString(assignmentCount),
                Integer.toString(validationCount),
                Integer.toString(printingCount),
                Integer.toString(qualityControlCount));

        write(elementsWriter, line);
    }

    /** Vuelca y cierra ambos archivos. Llamar despues de que todos los hilos terminaron. */
    public static synchronized void close() throws IOException {
        try {
            if (eventsWriter != null) eventsWriter.close();
        } finally {
            if (elementsWriter != null) elementsWriter.close();
            eventsWriter = null;
            elementsWriter = null;
        }
    }

    // ----------------------------------------------------------------------------------------
    // Privados: siempre se ejecutan con el monitor Logger.class ya tomado

    private static void writeEvent(String thread, int orderId, StageState stage, EventState event,
                                   OrderState from, OrderState to, String printerId) {
        sequence++;
        long elapsed = Math.max(0, System.currentTimeMillis() - startMillis);

        String line = String.join(";",
                Long.toString(sequence),
                Long.toString(elapsed),
                thread,
                Integer.toString(orderId),
                stage.name(),
                event.name(),
                from == null ? "" : from.name(),
                to == null ? "" : to.name(),
                nullToEmpty(printerId));

        write(eventsWriter, line);
    }

    private static void write(BufferedWriter writer, String line) {
        if (writer == null) {
            throw new IllegalStateException("Logger no inicializado: llamar a Logger.init(...) primero");
        }
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en el archivo de resultados", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
