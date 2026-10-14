package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.util.ArrayList;
import java.util.List;

public class OrderSource {
    private static final Object LOCK = new Object();
    private static List<PrintOrder> orders = new ArrayList<>();
    private static int terminalCount;
    private static int totalOrders;

    public OrderSource(int OrderAmount) {
        synchronized (LOCK) {
            orders = new ArrayList<>();
            terminalCount = 0;
            totalOrders = OrderAmount;
            for (int i = 1; i <= totalOrders; i++) orders.add(new PrintOrder(i));
            // TODO acá se loguea ORDER_CREATED de cada una
            // tendre que hacer cambios una vez que la clase Logger este lista
        }
    }

    public static PrintOrder getOrderInState(OrderState wanted) throws InterruptedException {
        synchronized (LOCK) {
            while (true) {
                for (PrintOrder o : orders) {
                    if (o.getCurrentState() == wanted && !o.processing) { o.processing = true; return o; }
                }
                if (terminalCount == orders.size()) return null;   // el worker termina
                LOCK.wait();
            }
        }
    }

    public void SetOrderState(PrintOrder order,OrderState newState, String threadName) {
        synchronized (LOCK) {
            order.SetState(newState);
            order.processing = false;

            if (newState.isTerminal()) {
                terminalCount++;

                // EVENTO ORDEN TERMINADA
                // TODO escribir la fila de eventos.csv (dentro del lock, para respetar el orden)
                if (terminalCount == totalOrders){
                    // EVENTO TODAS LAS ORDENES YA TERMINADAS
                }
            }

            //  EVENTO CAMBIO DE ESTADO
            LOCK.notifyAll();
        }
    }
}