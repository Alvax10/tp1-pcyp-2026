package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.util.ArrayList;
import java.util.List;

public class OrderSource {
    private final Object LOCK = new Object();
    private List<PrintOrder> orders = new ArrayList<>();
    private int totalOrders;
    private int terminalCount = 0;

    public OrderSource(int OrderAmount) {
        synchronized (LOCK) {
            orders = new ArrayList<>();
            totalOrders = OrderAmount;
            for (int i = 1; i <= totalOrders; i++)orders.add(new PrintOrder(i));
            } 
    }

    public PrintOrder getOrderInState(OrderState wanted) throws InterruptedException {
        synchronized (LOCK) {
            while (true) {
                for (PrintOrder o : orders) {
                    if (o.getCurrentState() == wanted && !o.processing) { 

                        switch (wanted) {
                            case OrderState.CREATED: o.assignmentCount ++; break;
                            case OrderState.WAITING_VALIDATION: o.validationCount ++; break;
                            case OrderState.READY_TO_PRINT: o.printingCount ++; break;
                            case OrderState.PRINTED: o.qualityControlCount ++; break;
                            default: break;
                        }
                        
                        o.processing = true; 
                        return o;
                    }
                }
                if (terminalCount == orders.size()) return null;   // el worker termina
                LOCK.wait();
            }
        }
    }

    public void SetOrderState(PrintOrder order,OrderState newState, Worker worker) {
        SetOrderState(order, newState, worker, null);
    }

    public void SetOrderState(PrintOrder order,OrderState newState, Worker worker, Runnable printerAction) {
        synchronized (LOCK) {
            if (printerAction != null) printerAction.run();
            OrderState prev = order.getCurrentState();
            order.SetState(newState);
            order.processing = false;

            // Se registra DENTRO del lock: las filas de eventos.csv quedan en el mismo orden
            // en que se confirman las transiciones. La secuencia la numera el Logger.
            Logger.recordEvent(
                worker,
                order.getId(),
                prev, newState,
                order.getPrinter() == null ? null : order.getPrinter().getPrinterId()
            );
            
            if (newState.isTerminal()) {
                terminalCount++;
            }
            
            //  EVENTO CAMBIO DE ESTADO
            LOCK.notifyAll();
        }
    }

    public List<PrintOrder> getAllOrders(){
        synchronized (LOCK){
            return new ArrayList<>(orders);
        }
    }
}
