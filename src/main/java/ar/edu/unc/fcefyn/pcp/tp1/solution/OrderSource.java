package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import java.util.ArrayList;

public class OrderSource {

    private final int totalOrders;
    private int terminalCount = 0;
    private ArrayList<PrintOrder> allOrders;
    private final OrderQueue ordersCreated;
    private final OrderQueue ordersWaitingValidation;
    private final OrderQueue ordersReadyToPrint;
    private final OrderQueue ordersPrinted;
    private boolean closed;

    public OrderSource(int OrderAmount) {

        totalOrders = OrderAmount;
        closed = false;
        allOrders = new ArrayList<>();

        ordersCreated = new OrderQueue();
        for (int i = 1; i <= OrderAmount; i++) {
            ordersCreated.putOrder(new PrintOrder(i));
        }

        ordersWaitingValidation = new OrderQueue();
        ordersReadyToPrint = new OrderQueue();
        ordersPrinted = new OrderQueue();
    }

    public PrintOrder getOrderInState(OrderState wanted) throws InterruptedException, IllegalArgumentException {

        closed = (terminalCount == totalOrders);
        if (closed) {

            ordersCreated.Close();
            ordersWaitingValidation.Close();
            ordersReadyToPrint.Close();
            ordersPrinted.Close();
            return null;
        }

        return switch (wanted) {
            case OrderState.CREATED -> ordersCreated.takeOrder();
            case OrderState.WAITING_VALIDATION -> ordersWaitingValidation.takeOrder();
            case OrderState.READY_TO_PRINT -> ordersReadyToPrint.takeOrder();
            case OrderState.PRINTED -> ordersPrinted.takeOrder();
            default -> throw new IllegalArgumentException("La transición: " + wanted + " no es soportada \n");
        };
    }

    public void SetOrderState(PrintOrder order,OrderState newState, Worker worker) {
        SetOrderState(order, newState, worker, null);
    }

    public void SetOrderState(PrintOrder order,OrderState newState, Worker worker, Runnable printerAction) {

        if (printerAction != null) printerAction.run();
        OrderState prev = order.getCurrentState();
        order.SetState(newState);

        switch (newState) {
            case OrderState.CREATED: ordersCreated.putOrder(order);break;
            case OrderState.WAITING_VALIDATION: ordersWaitingValidation.putOrder(order);break;
            case OrderState.READY_TO_PRINT: ordersReadyToPrint.putOrder(order);break;
            case OrderState.PRINTED: ordersPrinted.putOrder(order); break;
            case OrderState.APPROVED: break;
            case OrderState.DEFECTIVE: break;
            case OrderState.REJECTED: break;
            case OrderState.PRINT_FAILED: break;
            default:
                    throw new IllegalArgumentException("No existe el estado: " + newState);
        }

        // Se registra DENTRO del lock: las filas de eventos.csv quedan en el mismo orden
        // en que se confirman las transiciones. La secuencia la numera el Logger.

        if (worker == null) {
            Logger.recordCreated(order.getId());

        } else {
            Logger.recordEvent(worker, order.getId(), prev, order.getCurrentState(),
                    order.getPrinter() == null ? null : order.getPrinter().getPrinterId());
        }

        if (newState.isTerminal()) {
            terminalCount++;
            allOrders.add(order);
        }
    }

    public ArrayList<PrintOrder> getAllOrders() {
        return allOrders;
    }
}