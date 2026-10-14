package ar.edu.unc.fcefyn.pcp.tp1.solution;

public class AssignWorker extends Worker{
    private final OrdenSource orderSource;
    private final PrinterFarm printerFarm;

    AssignWorker(Stage stage, String name, long delayMs, OrdenSource orderSource, PrinterFarm printerFarm){
        super(stage, name,delayMs);
        this.orderSource = orderSource;
        this.printerFarm = printerFarm;
    }

    @Override
    protected void process() throws InterruptedException{
        //Tomo una orden en estado CREATED
        PrintOrder printOrder = ordenSource.getOrderInState(OrderState.CREATED);
        //La reservo de manera exclusiva
        printerFarm.reserve(printOrder.getId());
        //Cambio la orden a WAITING_VALIDATION
        orderSource.setNewState(printOrder, OrderState.WAITING_VALIDATION);
    }
}
