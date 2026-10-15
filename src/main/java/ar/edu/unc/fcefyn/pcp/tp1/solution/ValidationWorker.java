package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/** --------------------------- STAGE 2 - VALIDATION ------------------------------ **/

public class ValidationWorker extends Worker {

    private final OrderSource orderSource;
    private final SimulationConfig simulationConfig;
    private final PrinterFarm printerFarm;

    ValidationWorker(StageState stage, String name, long delayMs, OrderSource orderSource,
                     SimulationConfig simulationConfig, PrinterFarm printerFarm) {
        super(stage, name, delayMs);
        this.orderSource = orderSource;
        this.simulationConfig = simulationConfig;
        this.printerFarm = printerFarm;
    }

    @Override
    protected boolean process() throws InterruptedException {
        //Tomo una orden en WAITING_VALIDATION
        PrintOrder printOrder = orderSource.getOrderInState(OrderState.WAITING_VALIDATION);
        //null = todas las ordenes ya son terminales: no queda trabajo, el worker termina
        if (printOrder == null) {
            return false;
        }

        //Tiempo de la etapa, fuera de cualquier lock
        simulateWork();

        //Decision determinista (no toca estado compartido)
        if (OutcomeDecider.isModelValid(printOrder.getId(), simulationConfig)) {
            //Modelo valido: WAITING_VALIDATION -> READY_TO_PRINT, la impresora permanece reservada
            orderSource.SetOrderState(printOrder, OrderState.READY_TO_PRINT, this);
        } else {
            //Modelo invalido: WAITING_VALIDATION -> REJECTED y se libera la impresora DE ESTA ORDEN
            //(RESERVED -> AVAILABLE, availableCount++ y notifyAll para los AssignWorker que esperan).
            //Liberacion y transicion ocurren atomicamente dentro del lock de OrderSource:
            //nunca hay una orden terminal con impresora reservada (invariante 6).
            //La orden conserva la impresora como dato historico (elementos.csv).
            Printer printer = printOrder.getPrinter();
            orderSource.SetOrderState(printOrder, OrderState.REJECTED, this,
                    () -> printerFarm.release(printer));
        }
        return true;
    }
}
