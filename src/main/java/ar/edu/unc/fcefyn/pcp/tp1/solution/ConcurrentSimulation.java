package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.Simulation;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada obligatorio para la implementacion del grupo.
 */
public final class ConcurrentSimulation implements Simulation {

    @Override
    public SimulationResult execute(SimulationConfig config) throws InterruptedException {
        //Se guarda la hora de inicio
        long inicio = System.nanoTime();
        //Creacion de los recursos compartidos
        OrderSource orderSource = new OrderSource(config.totalOrders());
        PrinterFarm printerFarm = new PrinterFarm(config.printerRows(),config.printerColumns());
        Logger.init();

        /*Registro de la creacion de las 500 ordenes - Voy a recorrer la lista de ordenes
        * Las ordenes nacen cuando arranca la simulación, y quien arranca la simulación es execute. Es el momento
        * antes de que se creen los hilos. Después de eso, cada worker anota solo los cambios que él mismo provoca
        * (CREATED → WAITING_VALIDATION, etc.)
         */
        int secuencia = 0;
        for(PrintOrder order : orderSource.getAllOrders()){
            long elapsedMs = (System.nanoTime() - inicio) / 1_000_000; //calcula cuánto tiempo pasó desde que arrancó la simulación
            Logger.recordEvent(secuencia++, elapsedMs,Thread.currentThread(), order.getId(),StageState.INITIALIZATION, EventState.ORDER_CREATED,
                    null, OrderState.CREATED,null);
        }

        /*Crear los workers de cada etapa, con la cantidad y la demora del config:
        * AssignWorker (assignmentThreads, assignmentDelayMillis),
        * ValidationWorker (validationThreads, validationDelayMillis),
        * PrintingWorker (printingThreads, printingDelayMillis)
        * QualityWorker (qualityControlThreads, qualityControlDelayMillis).
        */
        List<Worker> workers = new ArrayList<>();
        for(int i = 0; i <= config.assignmentThreads(); i++){
            workers.add(new AssignWorker(StageState.ASSIGNMENT, "assignment-" + i , config.assignmentDelayMillis(),
                    orderSource, printerFarm));
        }
        for(int m = 0; m <= config.validationThreads(); m++){
            workers.add(new ValidationWorker(StageState.ASSIGNMENT, "validation-" + m, config.validationDelayMillis(),orderSource,
                    config,printerFarm));
        }
        for(int i = 0; i <= config.printingThreads(); i++){
            workers.add(new PrintingWorker(StageState.ASSIGNMENT, "printing-" + i, config.printingDelayMillis(),
                    orderSource, config, printerFarm));
        }
        for(int i = 0; i <= config.qualityControlThreads(); i++){
            workers.add(new QualityWorker(StageState.ASSIGNMENT, "quality-" + i, config.qualityControlDelayMillis(),
                    orderSource, config));
        }

        /*
            Creacion de los hilos, una lista de hilos para asignarles a cada worker
        *   Recorro la lista de workers y le asigno un hilo
        */
        List<Thread> threads = new ArrayList<>();
        for(Worker worker : workers){
            //Creo el hilo y le paso el worker correspondiente y su nombre
            Thread thread = new Thread( worker, worker.getName());
            threads.add(thread);
            //Arranco los hilos
            thread.start();
        }
        try{
            for(Thread thread : threads){
                thread.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Se interrumpieron los hilos antes de terminar");
        }

        throw new UnsupportedOperationException(
                "La simulacion concurrente todavia no fue implementada"
        );
    }
}

