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
        //Tomo una orden en estado CREATED
        PrintOrder printOrder = orderSource.getOrderInState(OrderState.CREATED);
        //La reservo de manera exclusiva
        printerFarm.reserve(printOrder);
        //Cambio la orden a WAITING_VALIDATION
        orderSource.SetOrderState(printOrder, OrderState.WAITING_VALIDATION, this);
    }
}
