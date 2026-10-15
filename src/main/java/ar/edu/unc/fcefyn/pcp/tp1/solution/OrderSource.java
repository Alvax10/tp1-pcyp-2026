package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro compartido de todas las ordenes. Es un monitor: un unico LOCK protege la lista,
 * el estado de cada orden, el flag processing, los contadores por orden y terminalCount.
 *
 * Espera/despertar:
 *  - getOrderInState: si no hay orden en el estado buscado, el hilo hace LOCK.wait().
 *  - SetOrderState: despues de cada transicion hace LOCK.notifyAll(), porque la orden
 *    pudo pasar a un estado que otra etapa esta esperando, o pudo ser la ultima terminal.
 *
 * Terminacion: cuando terminalCount == totalOrders, getOrderInState devuelve null y el
 * worker que lo recibe sale de su loop.
 *
 * Orden de locks (evita deadlock): si hace falta tocar una impresora junto con la transicion,
 * se toma primero LOCK y despues el monitor de PrinterFarm, nunca al reves. PrinterFarm no
 * llama nunca a OrderSource, y AssignWorker reserva la impresora SIN tener LOCK.
 */
public class OrderSource {
    private final Object LOCK = new Object();
    private final List<PrintOrder> orders;   // ordenada por id (1..totalOrders)
    private final int totalOrders;
    private int terminalCount = 0;

    public OrderSource(int totalOrders) {
        this.totalOrders = totalOrders;
        this.orders = new ArrayList<>(totalOrders);
        for (int i = 1; i <= totalOrders; i++) {
            orders.add(new PrintOrder(i));
        }
    }

    /**
     * Toma (de forma exclusiva) una orden en el estado pedido. Bloquea si no hay ninguna.
     * Devuelve null cuando todas las ordenes ya son terminales: no queda trabajo.
     */
    public PrintOrder getOrderInState(OrderState wanted) throws InterruptedException {
        synchronized (LOCK) {
            while (true) {
                for (PrintOrder o : orders) {
                    if (o.getCurrentState() == wanted && !o.isProcessing()) {
                        o.setProcessing(true);   // ningun otro hilo puede tomarla (invariante 3)
                        o.countStage(wanted);    // una pasada mas por esta etapa
                        return o;
                    }
                }
                if (terminalCount == totalOrders) {
                    return null;
                }
                LOCK.wait();
            }
        }
    }

    /**
     * Confirma la transicion de una orden, la libera (processing = false), registra el evento
     * DENTRO del lock (asi eventos.csv queda en el orden en que se confirman las transiciones)
     * y despierta a todos los que esperan.
     */
    public void SetOrderState(PrintOrder order, OrderState newState, Worker worker) {
        SetOrderState(order, newState, worker, null);
    }

    /**
     * Igual que SetOrderState, pero ejecuta printerAction (liberar o dar de baja la impresora)
     * dentro del mismo lock y ANTES de confirmar el nuevo estado. Asi la liberacion de la
     * impresora y la transicion de la orden son atomicas para el resto de los hilos:
     *  - nunca se ve una orden terminal con impresora reservada (invariante 6), y
     *  - la impresora no puede reasignarse (y loguearse) antes de que quede registrada la
     *    transicion de la orden que la estaba usando (eventos.csv consistente, invariante 4).
     */
    public void SetOrderState(PrintOrder order, OrderState newState, Worker worker, Runnable printerAction) {
        synchronized (LOCK) {
            if (printerAction != null) {
                printerAction.run();   // LOCK -> monitor de PrinterFarm (orden de locks fijo)
            }
            OrderState prev = order.getCurrentState();
            order.setState(newState);
            order.setProcessing(false);

            Logger.recordEvent(worker, order.getId(), prev, newState, order.getPrinterId());

            if (newState.isTerminal()) {
                terminalCount++;
            }
            LOCK.notifyAll();
        }
    }

    /** Copia de la lista (ordenada por id). Para leer el resultado despues de los join(). */
    public List<PrintOrder> getAllOrders() {
        synchronized (LOCK) {
            return new ArrayList<>(orders);
        }
    }

    public int getTerminalCount() {
        synchronized (LOCK) {
            return terminalCount;
        }
    }
}
