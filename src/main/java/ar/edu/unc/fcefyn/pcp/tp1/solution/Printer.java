package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;
import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;

/**
 * Impresora de la granja. NO es thread-safe por si sola: solo se modifica desde los metodos
 * synchronized de PrinterFarm (monitor de la granja).
 */
public class Printer {
    private final String printerId;
    private PrinterState currentState = PrinterState.AVAILABLE;
    private int usageCount = 0;
    private Integer assignedOrderId = null;

    public Printer(int row, int col) {
        printerId = "P-" + row + "-" + col;
    }

    /** AVAILABLE -> RESERVED: suma un uso y guarda la orden asignada. */
    void reserve(int orderId) {
        if (currentState != PrinterState.AVAILABLE) {
            throw new IllegalStateException(printerId + " no esta disponible (" + currentState + ")");
        }
        currentState = PrinterState.RESERVED;
        usageCount++;
        assignedOrderId = orderId;
    }

    /** RESERVED -> AVAILABLE: queda libre y sin orden asignada. */
    void release() {
        requireReserved();
        currentState = PrinterState.AVAILABLE;
        assignedOrderId = null;
    }

    /** RESERVED -> OUT_OF_SERVICE: no se vuelve a usar en esta ejecucion. */
    void outOfService() {
        requireReserved();
        currentState = PrinterState.OUT_OF_SERVICE;
        assignedOrderId = null;
    }

    private void requireReserved() {
        if (currentState != PrinterState.RESERVED) {
            throw new IllegalStateException(printerId + " no esta reservada (" + currentState + ")");
        }
    }

    public boolean isAvailable() { return currentState == PrinterState.AVAILABLE; }
    public String getPrinterId() { return printerId; }
    public PrinterState getCurrentState() { return currentState; }
    public int getUsageCount() { return usageCount; }
    public Integer getAssignedOrderId() { return assignedOrderId; }

    PrinterSnapshot toSnapshot() {
        return new PrinterSnapshot(printerId, currentState, usageCount, assignedOrderId);
    }
}
