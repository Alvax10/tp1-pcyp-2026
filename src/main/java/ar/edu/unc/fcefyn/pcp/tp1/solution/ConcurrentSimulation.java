package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.Simulation;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Punto de entrada obligatorio para la implementacion del grupo.
 */
public final class ConcurrentSimulation implements Simulation {

    @Override
    public SimulationResult execute(SimulationConfig config) throws InterruptedException {
        //Se guarda la hora de inicio
        long inicio = System.nanoTime();

        try {
            Logger.init(config.outputDirectory(), System.currentTimeMillis());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron crear los archivos de resultados", e);
        }

        //Creacion de los recursos compartidos
        OrderSource orderSource = new OrderSource(config.totalOrders());
        PrinterFarm printerFarm = new PrinterFarm(config.printerRows(),config.printerColumns());

        /*Crear los workers de cada etapa, con la cantidad y la demora del config:
         * AssignWorker (assignmentThreads, assignmentDelayMillis),
         * ValidationWorker (validationThreads, validationDelayMillis),
         * PrintingWorker (printingThreads, printingDelayMillis)
         * QualityWorker (qualityControlThreads, qualityControlDelayMillis).
         */
        List<Worker> workers = new ArrayList<>();
        for(int i = 1; i <= config.assignmentThreads(); i++){
            workers.add(new AssignWorker(StageState.ASSIGNMENT, "assignment-" + i , config.assignmentDelayMillis(),
                    orderSource, printerFarm));
        }
        for(int m = 1; m <= config.validationThreads(); m++){
            workers.add(new ValidationWorker(StageState.VALIDATION, "validation-" + m, config.validationDelayMillis(),orderSource,
                    config,printerFarm));
        }
        for(int i = 1; i <= config.printingThreads(); i++){
            workers.add(new PrintingWorker(StageState.PRINTING, "printing-" + i, config.printingDelayMillis(),
                    orderSource, config, printerFarm));
        }
        for(int i = 1; i <= config.qualityControlThreads(); i++){
            workers.add(new QualityWorker(StageState.QUALITY_CONTROL, "quality-" + i, config.qualityControlDelayMillis(),
                    orderSource, config));
        }

        /*
            Creacion de los hilos, una lista de hilos para asignarles a cada worker
        *   Recorro la lista de workers y le asigno un hilo
        */
        List<Thread> threads = new ArrayList<>();
        for(Worker worker : workers) {
            //Creo el hilo y le paso el worker correspondiente y su nombre
            Thread thread = new Thread(worker, worker.getName());
            threads.add(thread);
            //Arranco los hilos
            thread.start();
        }
        try{
            for(Thread thread : threads) {
                thread.join();
            }

        } catch (InterruptedException e) {
            System.out.println("Se interrumpieron los hilos antes de terminar");
            throw e; // no devolvemos un resultado con hilos todavia vivos
        }

        long durationMillis = (System.nanoTime() - inicio) / 1_000_000;
        boolean allThreadsTerminated = threads.stream().noneMatch(Thread::isAlive);


        /*
            Resultados: ya terminaron todos los hilos (join), asi que solo main toca estos datos.
        *   Recorro las ordenes (ordenadas por id): cuento por estado, armo los snapshots y escribo elementos.csv
        */
        List<OrderSnapshot> orderSnapshots = new ArrayList<>();
        Map<OrderState, Integer> totalsByState = new EnumMap<>(OrderState.class);
        int processedOrders = 0;


        for(PrintOrder order : orderSource.getAllOrders()) {

            OrderState state = order.getCurrentState();
            String printerId = order.getPrinter() == null ? null : order.getPrinter().getPrinterId();
            totalsByState.merge(state, 1, Integer::sum);
            if(state.isTerminal()) processedOrders++;

            orderSnapshots.add(new OrderSnapshot(order.getId(), state, printerId, order.getAssignmentCount(),
                    order.getValidationCount(), order.getPrintingCount(), order.getQualityControlCount()));

            Logger.recordElement(order.getId(), state, printerId, order.getAssignmentCount(),
                    order.getValidationCount(), order.getPrintingCount(), order.getQualityControlCount());
        }

        //Cierro los archivos y escribo resumen.properties
        try {

            Logger.close();
            try (BufferedWriter writer = Files.newBufferedWriter(config.outputDirectory().resolve("resumen.properties"))) {
                writer.write("totalOrders=" + config.totalOrders()); writer.newLine();
                writer.write("processedOrders=" + processedOrders); writer.newLine();
                writer.write("approvedOrders=" + totalsByState.getOrDefault(OrderState.APPROVED, 0)); writer.newLine();
                writer.write("rejectedOrders=" + totalsByState.getOrDefault(OrderState.REJECTED, 0)); writer.newLine();
                writer.write("printFailedOrders=" + totalsByState.getOrDefault(OrderState.PRINT_FAILED, 0)); writer.newLine();
                writer.write("defectiveOrders=" + totalsByState.getOrDefault(OrderState.DEFECTIVE, 0)); writer.newLine();
                writer.write("durationMillis=" + durationMillis); writer.newLine();
                writer.write("allThreadsTerminated=" + allThreadsTerminated); writer.newLine();
                writer.write("remainingIntermediateOrders=" + (config.totalOrders() - processedOrders)); writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron escribir los archivos de resultados", e);
        }

        return new SimulationResult(config.totalOrders(), processedOrders, totalsByState, orderSnapshots,
                printerFarm.snapshots(), durationMillis, allThreadsTerminated);
    }
}
