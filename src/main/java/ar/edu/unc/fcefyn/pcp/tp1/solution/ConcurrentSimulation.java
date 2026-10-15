package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.Simulation;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Punto de entrada obligatorio para la implementacion del grupo.
 *
 * Flujo:
 *  1. Crea los recursos compartidos (OrderSource, PrinterFarm) y abre el Logger.
 *  2. Registra la creacion de todas las ordenes (hilo main, stage INITIALIZATION).
 *  3. Arranca TODOS los hilos de las 4 etapas a la vez (ninguna etapa espera a otra).
 *  4. join() de todos: los workers terminan solos cuando OrderSource les devuelve null
 *     (todas las ordenes terminales). No se usa Thread.stop ni System.exit.
 *  5. Con todos los hilos terminados, arma los snapshots y escribe elementos.csv y resumen.properties.
 */
public final class ConcurrentSimulation implements Simulation {

    @Override
    public SimulationResult execute(SimulationConfig config) throws InterruptedException {
        long startNanos = System.nanoTime();

        // 1. Recursos compartidos
        OrderSource orderSource = new OrderSource(config.totalOrders());
        PrinterFarm printerFarm = new PrinterFarm(config.printerRows(), config.printerColumns());
        try {
            Logger.init(config.outputDirectory(), System.currentTimeMillis());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron crear los archivos de resultados", e);
        }

        List<Thread> threads = new ArrayList<>();
        try {
            // 2. Las ordenes nacen antes de arrancar los hilos: fila ORDER_CREATED por cada una
            for (PrintOrder order : orderSource.getAllOrders()) {
                Logger.recordCreated(order.getId());
            }

            // 3. Workers de cada etapa, con la cantidad de hilos y la demora de la configuracion
            List<Worker> workers = new ArrayList<>();
            for (int i = 1; i <= config.assignmentThreads(); i++) {
                workers.add(new AssignWorker(StageState.ASSIGNMENT, "assignment-" + i,
                        config.assignmentDelayMillis(), orderSource, printerFarm));
            }
            for (int i = 1; i <= config.validationThreads(); i++) {
                workers.add(new ValidationWorker(StageState.VALIDATION, "validation-" + i,
                        config.validationDelayMillis(), orderSource, config, printerFarm));
            }
            for (int i = 1; i <= config.printingThreads(); i++) {
                workers.add(new PrintingWorker(StageState.PRINTING, "printing-" + i,
                        config.printingDelayMillis(), orderSource, config, printerFarm));
            }
            for (int i = 1; i <= config.qualityControlThreads(); i++) {
                workers.add(new QualityWorker(StageState.QUALITY_CONTROL, "quality-" + i,
                        config.qualityControlDelayMillis(), orderSource, config));
            }

            for (Worker worker : workers) {
                Thread thread = new Thread(worker, worker.getName()); // el nombre va a la columna thread
                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.start();
            }

            // 4. Espera a que terminen todos. Si interrumpen a main, la excepcion se propaga
            //    (execute declara throws InterruptedException) en vez de devolver un resultado a medias.
            for (Thread thread : threads) {
                thread.join();
            }
        } finally {
            if (threads.stream().anyMatch(Thread::isAlive)) {
                // Solo en caso de error/interrupcion: pedimos a los workers que salgan
                threads.forEach(Thread::interrupt);
            }
        }

        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
        boolean allThreadsTerminated = threads.stream().noneMatch(Thread::isAlive);

        // 5. Resultados (ya no hay hilos concurrentes: join() garantiza la visibilidad)
        List<PrintOrder> orders = orderSource.getAllOrders();   // ordenadas por id
        List<OrderSnapshot> orderSnapshots = new ArrayList<>();
        Map<OrderState, Integer> totalsByState = new EnumMap<>(OrderState.class);
        int processedOrders = 0;

        for (PrintOrder order : orders) {
            OrderState state = order.getCurrentState();
            totalsByState.merge(state, 1, Integer::sum);
            if (state.isTerminal()) {
                processedOrders++;
            }
            orderSnapshots.add(new OrderSnapshot(order.getId(), state, order.getPrinterId(),
                    order.getAssignmentCount(), order.getValidationCount(),
                    order.getPrintingCount(), order.getQualityControlCount()));
            Logger.recordElement(order.getId(), state, order.getPrinterId(),
                    order.getAssignmentCount(), order.getValidationCount(),
                    order.getPrintingCount(), order.getQualityControlCount());
        }

        try {
            Logger.close();
            writeSummary(config.outputDirectory(), config.totalOrders(), processedOrders, totalsByState,
                    durationMillis, allThreadsTerminated, config.totalOrders() - processedOrders);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron escribir los archivos de resultados", e);
        }

        return new SimulationResult(config.totalOrders(), processedOrders, totalsByState,
                orderSnapshots, printerFarm.snapshots(), durationMillis, allThreadsTerminated);
    }

    private static void writeSummary(Path outputDirectory, int totalOrders, int processedOrders,
                                     Map<OrderState, Integer> totals, long durationMillis,
                                     boolean allThreadsTerminated, int remainingIntermediateOrders)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputDirectory.resolve("resumen.properties"), StandardCharsets.UTF_8)) {
            writer.write("totalOrders=" + totalOrders); writer.newLine();
            writer.write("processedOrders=" + processedOrders); writer.newLine();
            writer.write("approvedOrders=" + totals.getOrDefault(OrderState.APPROVED, 0)); writer.newLine();
            writer.write("rejectedOrders=" + totals.getOrDefault(OrderState.REJECTED, 0)); writer.newLine();
            writer.write("printFailedOrders=" + totals.getOrDefault(OrderState.PRINT_FAILED, 0)); writer.newLine();
            writer.write("defectiveOrders=" + totals.getOrDefault(OrderState.DEFECTIVE, 0)); writer.newLine();
            writer.write("durationMillis=" + durationMillis); writer.newLine();
            writer.write("allThreadsTerminated=" + allThreadsTerminated); writer.newLine();
            writer.write("remainingIntermediateOrders=" + remainingIntermediateOrders); writer.newLine();
        }
    }
}
