package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class OrderSource {

    private final List<PrintOrder> allOrders = new ArrayList<>();
    private final int totalOrders;

    // ---- 4 colas independientes, cada una con su propio monitor ----
    private final Deque<PrintOrder> createdQueue    = new ArrayDeque<>();
    private final Deque<PrintOrder> validationQueue = new ArrayDeque<>();
    private final Deque<PrintOrder> printingQueue   = new ArrayDeque<>();
    private final Deque<PrintOrder> qualityQueue    = new ArrayDeque<>();

    private final Object createdMonitor    = new Object();
    private final Object validationMonitor = new Object();
    private final Object printingMonitor   = new Object();
    private final Object qualityMonitor    = new Object();

    // ---- Monitor de estado terminal / fin de simulación ----
    private final Object terminalMonitor = new Object();
    private int terminalCount = 0;

    public OrderSource(int orderAmount) {
        this.totalOrders = orderAmount;
        for (int i = 1; i <= orderAmount; i++) {
            PrintOrder o = new PrintOrder(i);
            allOrders.add(o);
            createdQueue.addLast(o);
        }
    }

    // ------------------------------------------------------------------
    //  Consumo
    // ------------------------------------------------------------------
    public PrintOrder getOrderInState(OrderState wanted) throws InterruptedException {
        switch (wanted) {
            case CREATED:
                return takeFromQueue(createdQueue, createdMonitor, wanted);
            case WAITING_VALIDATION:
                return takeFromQueue(validationQueue, validationMonitor, wanted);
            case READY_TO_PRINT:
                return takeFromQueue(printingQueue, printingMonitor, wanted);
            case PRINTED:
                return takeFromQueue(qualityQueue, qualityMonitor, wanted);
            default:
                throw new IllegalArgumentException(
                        "Estado no soportado en getOrderInState: " + wanted);
        }
    }

    private PrintOrder takeFromQueue(Deque<PrintOrder> queue,
                                     Object monitor,
                                     OrderState wanted) throws InterruptedException {
        synchronized (monitor) {
            while (queue.isEmpty()) {
                if (isAllTerminal()) {
                    return null;          // no queda trabajo en el sistema
                }
                monitor.wait();
            }
            PrintOrder order = queue.pollFirst();
            // contadores de etapa (ajustá los nombres a los campos reales de PrintOrder)
            switch (wanted) {
                case CREATED:            order.assignmentCount++;      break;
                case WAITING_VALIDATION: order.validationCount++;      break;
                case READY_TO_PRINT:     order.printingCount++;        break;
                case PRINTED:            order.qualityControlCount++;  break;
                default: break;
            }
            return order;
        }
    }

    // ------------------------------------------------------------------
    //  Transiciones de estado
    // ------------------------------------------------------------------
    public void SetOrderState(PrintOrder order, OrderState newState, Worker worker) {
        SetOrderState(order, newState, worker, null);
    }

    public void SetOrderState(PrintOrder order, OrderState newState,
                              Worker worker, Runnable printerAction) {
        if (newState.isTerminal()) {
            setTerminalState(order, newState, worker, printerAction);
        } else {
            setIntermediateState(order, newState, worker, printerAction);
        }
    }

    private void setIntermediateState(PrintOrder order, OrderState newState,
                                      Worker worker, Runnable printerAction) {
        Deque<PrintOrder> targetQueue;
        Object targetMonitor;

        switch (newState) {
            case WAITING_VALIDATION:
                targetQueue = validationQueue; targetMonitor = validationMonitor; break;
            case READY_TO_PRINT:
                targetQueue = printingQueue;   targetMonitor = printingMonitor;   break;
            case PRINTED:
                targetQueue = qualityQueue;    targetMonitor = qualityMonitor;    break;
            default:
                throw new IllegalArgumentException(
                        "Transición intermedia no soportada: " + newState);
        }

        synchronized (targetMonitor) {
            if (printerAction != null) printerAction.run();   // atomicidad con release
            OrderState prev = order.getCurrentState();
            order.SetState(newState);
            targetQueue.addLast(order);
            targetMonitor.notifyAll();                         // despierta solo a esa cola
            Logger.recordEvent(worker, order.getId(), prev, newState,
                    order.getPrinter() == null ? null : order.getPrinter().getPrinterId());
        }
    }

    private void setTerminalState(PrintOrder order, OrderState newState,
                                  Worker worker, Runnable printerAction) {
        boolean finished;
        synchronized (terminalMonitor) {
            if (printerAction != null) printerAction.run();   // atomicidad con release
            OrderState prev = order.getCurrentState();
            order.SetState(newState);
            Logger.recordEvent(worker, order.getId(), prev, newState,
                    order.getPrinter() == null ? null : order.getPrinter().getPrinterId());
            terminalCount++;
            finished = (terminalCount == totalOrders);
            terminalMonitor.notifyAll();
        }
        // Fuera de terminalMonitor: despertar a los workers que duermen en cada cola.
        if (finished) {
            wakeAllQueueWaiters();
        }
    }

    private void wakeAllQueueWaiters() {
        synchronized (createdMonitor)    { createdMonitor.notifyAll();    }
        synchronized (validationMonitor) { validationMonitor.notifyAll(); }
        synchronized (printingMonitor)   { printingMonitor.notifyAll();   }
        synchronized (qualityMonitor)    { qualityMonitor.notifyAll();    }
    }

    private boolean isAllTerminal() {
        synchronized (terminalMonitor) {
            return terminalCount == totalOrders;
        }
    }

    public List<PrintOrder> getAllOrders() {
        return new ArrayList<>(allOrders);   // lista maestra inmutable post-construcción
    }
}
