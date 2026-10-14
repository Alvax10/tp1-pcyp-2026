package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;

/** --------------------------- STAGE 3 - PRINTING ------------------------------ **/

public class PrintingWorker extends Worker {

    private final OrderSource orderSource; // de donde toma (READY_TO_PRINT)
    private PrintOrder readyToPrintOrder;  // destino si imprime bien (hacia calidad)
    private Printer printer;               // destino si imprime bien (hacia calidad)

    public PrintingWorker(Stage stage, String name, long delayedMs, OrderSource orderSource) {

        super(stage,name,delayedMs);
        this.orderSource = orderSource;
    }

    @Override
    protected void process() throws InterruptedException {

        // ESTO VA PARA EL EXECUTE

        /**
        for (int i = 1; i <= 3; i++) {
            new Thread(new PrintingWorker("printing-" + i, 1000,
                    readyToPrint, printed, failed, printerPool)).start();
        }
        **/

        readyToPrintOrder = orderSource.getOrderInState(OrderState.READY_TO_PRINT); //  gets a READY_TO_PRINT order
        printer = readyToPrintOrder.getPrinter();                        // printer's order from previous stage
        Thread.sleep(getDelayMs()); // simulates the time that the workers take to do the task

        if (OutcomeDecider.isPrintSuccessful(readyToPrintOrder.getId())) {

            readyToPrintOrder.setNewState(readyToPrintOrder.getId(), OrderState.PRINTED); // READY_TO_PRINT -> PRINTED
            printer.release(); // RESERVED -> AVAILABLE

        } else {

            readyToPrintOrder.setNewState(readyToPrintOrder.getId(), OrderState.PRINT_FAILED); // READY_TO_PRINT -> PRINT_FAILED
            printer.setNewState(PrinterState.OUT_OF_SERVICE);   // RESERVED -> OUT_OF_SERVICE
        }
    }
}
