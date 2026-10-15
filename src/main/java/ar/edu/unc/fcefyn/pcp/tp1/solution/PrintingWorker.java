package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/** --------------------------- STAGE 3 - PRINTING ------------------------------ **/

public class PrintingWorker extends Worker {

    private final OrderSource orderSource;   // de donde toma (READY_TO_PRINT)
    private final PrinterFarm printerFarm;   // donde libera o da de baja la impresora
    private final SimulationConfig config;

    PrintingWorker(StageState stage, String name, long delayMs, OrderSource orderSource,
                   SimulationConfig config, PrinterFarm printerFarm) {
        super(stage, name, delayMs);
        this.orderSource = orderSource;
        this.printerFarm = printerFarm;
        this.config = config;
    }

    @Override
    protected boolean process() throws InterruptedException {
        //Tomo una orden READY_TO_PRINT
        PrintOrder order = orderSource.getOrderInState(OrderState.READY_TO_PRINT);
        //null = todas las ordenes ya son terminales: no queda trabajo, el worker termina
        if (order == null) {
            return false;
        }

        //Tiempo de impresion, fuera de cualquier lock (una sola demora por orden)
        simulateWork();

        //En ambas ramas la impresora se libera / da de baja en la MISMA operacion atomica que el
        //cambio de estado (dentro del lock de OrderSource): nunca hay una orden PRINTED/terminal
        //con su impresora todavia RESERVED (invariante 6)
        Printer printer = order.getPrinter();
        if (OutcomeDecider.isPrintSuccessful(order.getId(), config)) {
            // READY_TO_PRINT -> PRINTED       e   impresora RESERVED -> AVAILABLE
            orderSource.SetOrderState(order, OrderState.PRINTED, this,
                    () -> printerFarm.release(printer));
        } else {
            // READY_TO_PRINT -> PRINT_FAILED  e   impresora RESERVED -> OUT_OF_SERVICE
            orderSource.SetOrderState(order, OrderState.PRINT_FAILED, this,
                    () -> printerFarm.setOutOfService(printer));
        }
        return true;
    }
}
