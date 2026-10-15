package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.util.ArrayList;
import java.util.List;

public class OrderSource {
    private final Object LOCK = new Object();
    private List<PrintOrder> orders = new ArrayList<>();
    private int totalOrders;
    private int terminalCount, assignmentCount, validationCount, printingCount, qualityControlCount = 0;
    private int sequence = 1;

    public OrderSource(int OrderAmount) {
        synchronized (LOCK) {
            orders = new ArrayList<>();
            totalOrders = OrderAmount;
            for (int i = 1; i <= totalOrders; i++)orders.add(new PrintOrder(i));
            } 
    }

    public static PrintOrder getOrderInState(OrderState wanted) throws InterruptedException {
        synchronized (LOCK) {
            while (true) {
                for (PrintOrder o : orders) {
                    if (o.getCurrentState() == wanted && !o.processing) { 

                        switch (wanted) {
                            case OrderState.CREATED: assignmentCount ++; break;
                            case OrderState.WAITING_VALIDATION: validationCount ++; break;
                            case OrderState.READY_TO_PRINT: printingCount ++; break;
                            case OrderState.PRINTED: qualityControlCount ++; break;
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
        synchronized (LOCK) {
            OrderState prev = order.getCurrentState();
            order.SetState(newState);
            order.processing = false;

            switch (prev) {
                case OrderState.CREATED: assignmentCount --; break;
                case OrderState.WAITING_VALIDATION: validationCount --; break;
                case OrderState.READY_TO_PRINT: printingCount --; break;
                case OrderState.PRINTED: qualityControlCount --; break;
                default: break;
            }
            
            sequence++;
            
            Logger.recordEvent(
                sequence, 
                worker, 
                order.getId()
                EventState.ORDER_STATE_CHANGED, 
                prev, newState, 
                order.getPrinter().getPrinterId()
            );
            
            if (newState.isTerminal()) {
                terminalCount++;

                // EVENTO ORDEN TERMINADA
                Logger.recordElement(order.getId(), newState, order.getPrinter().getPrinterId(), assignmentCount, validationCount, printingCount, qualityControlCount);
                
                // TODO escribir la fila de eventos.csv (dentro del lock, para respetar el orden)
                if (terminalCount == totalOrders){
                    // EVENTO TODAS LAS ORDENES YA TERMINADAS
                }
            }
            
            //  EVENTO CAMBIO DE ESTADO
            LOCK.notifyAll();
        }
    }

    public List<PrintOrder> getAllOrders(){
        synchronized (LOCK){
            return orders = new ArrayList<>(orders);
        }
    }
}
