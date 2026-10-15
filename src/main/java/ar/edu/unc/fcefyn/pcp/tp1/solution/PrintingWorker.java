package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

/** --------------------------- STAGE 3 - PRINTING ------------------------------ **/

public class PrintingWorker extends Worker {

    private final OrderSource orderSource; // de donde toma (READY_TO_PRINT)
    // destino si imprime bien (hacia calidad)
    private PrinterFarm printerFarm;       // destino si imprime bien (hacia calidad
    private final SimulationConfig config;

    public PrintingWorker(StageState stage, String name, long delayedMs, OrderSource orderSource,
                          SimulationConfig config, PrinterFarm printerFarm) {

        super(stage,name,delayedMs);
        this.printerFarm = printerFarm;
        this.orderSource = orderSource;
        this.config = config;
    }

    @Override
    protected void process() throws InterruptedException {
        PrintOrder readyToPrintOrder = orderSource.getOrderInState(OrderState.READY_TO_PRINT); //  gets a READY_TO_PRINT order
                        // printer's order from previous stage
        // null = todas las ordenes ya son terminales: no queda trabajo, el worker termina
        if (readyToPrintOrder == null) {
            Thread.currentThread().interrupt();
            return;
        }
        // (la demora la aplica Worker.run() una vez por orden: aca no se duerme otra vez)

        if (OutcomeDecider.isPrintSuccessful(readyToPrintOrder.getId(), this.config)) {

            orderSource.SetOrderState(readyToPrintOrder, OrderState.PRINTED, this,   // READY_TO_PRINT -> PRINTED
                    () -> printerFarm.release(readyToPrintOrder.getPrinter()));      // RESERVED -> AVAILABLE (atomico)

        } else {

            Printer printer = readyToPrintOrder.getPrinter();
            orderSource.SetOrderState(readyToPrintOrder, OrderState.PRINT_FAILED, this,  // READY_TO_PRINT -> PRINT_FAILED
                                      ()->printerFarm.setOutOfService(printer));                     // RESERVED -> OUT_OF_SERVICE (atomico)
        }
    }
}
