package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/** --------------------------- STAGE 2 - VALIDATION ------------------------------ **/

public class ValidationWorker extends Worker{


    private final OrderSource orderSource;
    private final SimulationConfig simulationConfig;
    private final Printer printer;

    ValidationWorker(Stage stage, String name, long delayMs, OrderSource orderSource, SimulationConfig simulationConfig, Printer printer){
        super(stage, name, delayMs);
        this.orderSource = orderSource;
        this.simulationConfig = simulationConfig;
        this.printer = printer;
    }

    @Override
    protected void process() throws InterruptedException {
        //Tomo una orden en WAITNIG_VALIDATION
        PrintOrder printOrder = orderSource.getOrderInState(OrderState.WAITING_VALIDATION);
        if(OutcomeDecider.isModelValid(printOrder.getId(), simulationConfig)){
            //Si el modelo es valido:
            //Cambia la orden de WAITING_VALIDATION -> READY_TO_PRINT, la impresora permanece reservada
            orderSource.SetOrderState(printOrder,OrderState.READY_TO_PRINT, getName());
        }else{
            //Si el modelo no es valido:
            //Cambia la orden de WAITING_VALIDATION -> REJECTED, la orden queda terminada
            orderSource.SetOrderState(printOrder,OrderState.REJECTED, getName());
            //Cambia la impresora de RESERVED -> AVAILABLE, la impresora puede recibir otro trabajo
            printer.setState(PrinterState.AVAILABLE);
        }
    }
}
