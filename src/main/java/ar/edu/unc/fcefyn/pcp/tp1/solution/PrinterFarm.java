package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Matriz de impresoras compartida. Es un monitor (metodos synchronized sobre this):
 * protege la matriz, el estado de cada Printer y availableCount.
 *
 * Espera/despertar: reserve() hace wait() mientras no haya impresoras disponibles;
 * release() suma una disponible y hace notifyAll().
 * setOutOfService() no despierta a nadie: no agrega impresoras disponibles.
 */
public class PrinterFarm {
    private final Printer[][] matrix;
    private int availableCount;

    public PrinterFarm(int rows, int columns) {
        matrix = new Printer[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = new Printer(i, j);
            }
        }
        availableCount = rows * columns;
    }

    /**
     * Reserva de forma exclusiva una impresora disponible para la orden (AVAILABLE -> RESERVED,
     * suma un uso). Si no hay ninguna, el hilo duerme hasta que alguien haga release().
     */
    public synchronized Printer reserve(PrintOrder order) throws InterruptedException {
        while (availableCount == 0) {
            wait();
        }
        Printer printer = findAvailable();
        printer.reserve(order.getId());
        availableCount--;
        return printer;
    }

    /** RESERVED -> AVAILABLE y despierta a los AssignWorker que esperan una impresora. */
    public synchronized void release(Printer printer) {
        printer.release();
        availableCount++;
        notifyAll();
    }

    /**
     * RESERVED -> OUT_OF_SERVICE. No toca availableCount: ya se desconto al reservarla
     * y no vuelve a estar disponible.
     */
    public synchronized void setOutOfService(Printer printer) {
        printer.outOfService();
    }

    /** Estado de todas las impresoras (recorrido por filas). Llamar despues de los join(). */
    public synchronized List<PrinterSnapshot> snapshots() {
        List<PrinterSnapshot> result = new ArrayList<>();
        for (Printer[] row : matrix) {
            for (Printer printer : row) {
                result.add(printer.toSnapshot());
            }
        }
        return result;
    }

    /** Suma de usos de todas las impresoras (debe coincidir con el total de asignaciones). */
    public synchronized int totalUsages() {
        int total = 0;
        for (Printer[] row : matrix) {
            for (Printer printer : row) {
                total += printer.getUsageCount();
            }
        }
        return total;
    }

    // Solo se llama desde reserve(), con el lock ya tomado y availableCount > 0
    private Printer findAvailable() {
        for (Printer[] row : matrix) {
            for (Printer printer : row) {
                if (printer.isAvailable()) {
                    return printer;
                }
            }
        }
        throw new IllegalStateException("availableCount > 0 pero no se encontro impresora disponible");
    }
}
