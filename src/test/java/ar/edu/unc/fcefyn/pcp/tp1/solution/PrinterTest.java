package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios de Printer (sin concurrencia: Printer no se sincroniza sola,
 * la protege el monitor de PrinterFarm).
 */
class PrinterTest {

    @Test
    @DisplayName("Una impresora nueva tiene id P-fila-columna, esta AVAILABLE, sin usos y sin orden")
    void nuevaImpresoraArrancaDisponible() {
        Printer printer = new Printer(2, 7);

        assertEquals("P-2-7", printer.getPrinterId());
        assertEquals(PrinterState.AVAILABLE, printer.getCurrentState());
        assertTrue(printer.isAvailable());
        assertEquals(0, printer.getUsageCount());
        assertNull(printer.getAssignedOrderId());
    }

    @Test
    @DisplayName("reserve: pasa a RESERVED, suma un uso y guarda la orden")
    void reserveMarcaReservadaYGuardaOrden() {
        Printer printer = new Printer(0, 0);

        printer.reserve(42);

        assertEquals(PrinterState.RESERVED, printer.getCurrentState());
        assertFalse(printer.isAvailable());
        assertEquals(1, printer.getUsageCount());
        assertEquals(42, printer.getAssignedOrderId());
    }

    @Test
    @DisplayName("Liberar (AVAILABLE) limpia la orden asignada pero conserva el contador de usos")
    void liberarLimpiaOrdenYConservaUsos() {
        Printer printer = new Printer(0, 0);
        printer.reserve(1);

        printer.setState(PrinterState.AVAILABLE);

        assertTrue(printer.isAvailable());
        assertNull(printer.getAssignedOrderId());
        assertEquals(1, printer.getUsageCount());
    }

    @Test
    @DisplayName("El contador de usos acumula una unidad por cada reserva")
    void contadorDeUsosAcumula() {
        Printer printer = new Printer(0, 0);

        for (int orderId = 1; orderId <= 5; orderId++) {
            printer.reserve(orderId);
            assertEquals(orderId, printer.getAssignedOrderId());
            printer.setState(PrinterState.AVAILABLE);
        }

        assertEquals(5, printer.getUsageCount());
    }

    @Test
    @DisplayName("OUT_OF_SERVICE: deja de estar disponible y no conserva la orden")
    void fueraDeServicioNoEstaDisponible() {
        Printer printer = new Printer(1, 1);
        printer.reserve(9);

        printer.setState(PrinterState.OUT_OF_SERVICE);

        assertEquals(PrinterState.OUT_OF_SERVICE, printer.getCurrentState());
        assertFalse(printer.isAvailable());
        assertNull(printer.getAssignedOrderId());
        assertEquals(1, printer.getUsageCount());
    }

    @Test
    @DisplayName("isAvailable es true solo en estado AVAILABLE")
    void isAvailableSoloEnAvailable() {
        Printer printer = new Printer(0, 0);

        printer.setState(PrinterState.RESERVED);
        assertFalse(printer.isAvailable());

        printer.setState(PrinterState.OUT_OF_SERVICE);
        assertFalse(printer.isAvailable());

        printer.setState(PrinterState.AVAILABLE);
        assertTrue(printer.isAvailable());
    }
}
