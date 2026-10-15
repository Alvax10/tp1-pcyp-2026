package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

/** --------------------------- STAGE 1 - ASSIGN ------------------------------ **/

public class AssignWorker extends Worker{
    private final OrderSource orderSource;
    private final PrinterFarm printerFarm;

    AssignWorker(StageState stage, String name, long delayMs, OrderSource orderSource, PrinterFarm printerFarm){
        super(stage, name,delayMs);
        this.orderSource = orderSource;
        this.printerFarm = printerFarm;
    }

    @Override
    protected void process() throws InterruptedException{
        //Tomo una orden en estado CREATED (bloqueante: si no hay, duermo en el monitor de OrderSource)
        PrintOrder printOrder = orderSource.getOrderInState(OrderState.CREATED);

        //null = todas las ordenes ya son terminales: no queda trabajo, el worker termina
        if (printOrder == null) {
            Thread.currentThread().interrupt();
            return;
        }

        //Obtengo una impresora disponible y la reservo de manera exclusiva
        //(dentro del monitor de PrinterFarm: AVAILABLE -> RESERVED y suma un uso)
        //Si no hay ninguna disponible, el hilo duerme en PrinterFarm hasta que alguien haga release
        Printer printer = printerFarm.reserve(printOrder);

        //La asocio con la orden ANTES de publicar el cambio de estado: cuando otro hilo vea la orden
        //en WAITING_VALIDATION (a traves del lock de OrderSource) ya va a tener su impresora (invariante 5)
        printOrder.setPrinter(printer);

        //Cambio la orden a WAITING_VALIDATION
        orderSource.SetOrderState(printOrder, OrderState.WAITING_VALIDATION, this);
    }
}
