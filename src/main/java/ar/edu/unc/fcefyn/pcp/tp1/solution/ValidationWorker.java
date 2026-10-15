package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/** --------------------------- STAGE 2 - VALIDATION ------------------------------ **/

public class ValidationWorker extends Worker{

    private final OrderSource orderSource;
    private final SimulationConfig simulationConfig;
    private final PrinterFarm printerFarm;

    ValidationWorker(StageState stage, String name, long delayMs, OrderSource orderSource,
                     SimulationConfig simulationConfig, PrinterFarm printerFarm){
        super(stage, name, delayMs);
        this.orderSource = orderSource;
        this.simulationConfig = simulationConfig;
        this.printerFarm = printerFarm;
    }

    @Override
    protected void process() throws InterruptedException {
        //Tomo una orden en WAITING_VALIDATION
        PrintOrder printOrder = orderSource.getOrderInState(OrderState.WAITING_VALIDATION);

        //null = todas las ordenes ya son terminales: no queda trabajo, el worker termina
        if (printOrder == null) {
            Thread.currentThread().interrupt();
            return;
        }

        //Decision determinista (fuera de cualquier lock: no toca estado compartido)
        if(OutcomeDecider.isModelValid(printOrder.getId(), simulationConfig)){
            //Si el modelo es valido:
            //Cambia la orden de WAITING_VALIDATION -> READY_TO_PRINT, la impresora permanece reservada
            orderSource.SetOrderState(printOrder,OrderState.READY_TO_PRINT, this);
        }else{
            //Si el modelo no es valido:
            //1) Libero la impresora DE ESTA ORDEN a traves de PrinterFarm (RESERVED -> AVAILABLE).
            //   Va por el monitor de la granja para que availableCount++ y notifyAll() despierten
            //   a los AssignWorker que esperan una impresora.
            //   Se libera ANTES de pasar a REJECTED para que nunca exista una orden terminal con
            //   impresora reservada (invariante 6).
            printerFarm.release(printOrder.getPrinter());
            //2) Cambia la orden de WAITING_VALIDATION -> REJECTED, la orden queda terminada.
            //   La orden conserva la referencia a la impresora como dato historico (elementos.csv).
            orderSource.SetOrderState(printOrder,OrderState.REJECTED, this);
        }
    }
}
