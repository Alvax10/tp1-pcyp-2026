package ar.edu.unc.fcefyn.pcp.tp1.solution;

import java.util.ArrayList;
import java.util.List;

/* --------------------- SECCIÓN CRÍTICA PARA CADA STAGE ------------------------------------------- */

public class OrderQueue {

    private List<PrintOrder> orderStateList;
    private final Object orderStateKey;
    private boolean closed;

    public OrderQueue() {

        this.orderStateList = new ArrayList<>();
        orderStateKey = new Object();
        closed = false;
    }

    public PrintOrder takeOrder() throws InterruptedException {

        synchronized (orderStateKey) {

            while (orderStateList.isEmpty()) {
                if (closed) {
                    return  null;
                }
                orderStateKey.wait();
            }

            return orderStateList.removeFirst();
        }
    }

    public void putOrder(PrintOrder printOrder) {

        synchronized (orderStateKey) {

            printOrder.incrementStageCount();
            orderStateList.add(printOrder);
            orderStateKey.notifyAll();
        }
    }

    public void Close() {
        synchronized (orderStateKey) {
            closed = true;
            orderStateKey.notifyAll();
        }
    }

    public List<PrintOrder> getList() {
        return this.orderStateList;
    }
}
