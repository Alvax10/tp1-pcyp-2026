package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests de PrinterFarm: primero el comportamiento secuencial y despues la sincronizacion
 * (exclusion mutua en reserve/release y espera con wait/notifyAll cuando no hay impresoras).
 *
 * Los tests concurrentes llevan @Timeout: si hay un deadlock o un hilo que nunca se despierta,
 * el test falla en vez de colgar la suite.
 */
class PrinterFarmTest {

    // ------------------------------------------------------------------ secuenciales

    @Test
    @DisplayName("Al crearla, todas las impresoras estan disponibles y no hay usos")
    void granjaNuevaTodasDisponibles() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(3, 4);

        assertEquals(0, farm.totalOrders());

        // Se pueden reservar exactamente filas * columnas impresoras, todas distintas
        Set<String> ids = new HashSet<>();
        for (int i = 1; i <= 12; i++) {
            Printer p = farm.reserve(new PrintOrder(i));
            assertTrue(ids.add(p.getPrinterId()), "Se entrego dos veces la impresora " + p.getPrinterId());
        }
        assertEquals(12, ids.size());
    }

    @Test
    @DisplayName("Los ids de las impresoras respetan la posicion en la matriz (P-fila-columna)")
    void idsSegunPosicion() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(2, 3);
        Set<String> ids = new HashSet<>();
        for (int i = 1; i <= 6; i++) {
            ids.add(farm.reserve(new PrintOrder(i)).getPrinterId());
        }

        assertEquals(Set.of("P-0-0", "P-0-1", "P-0-2", "P-1-0", "P-1-1", "P-1-2"), ids);
    }

    @Test
    @DisplayName("reserve: la impresora queda RESERVED, asociada a la orden y con un uso")
    void reserveAsociaLaOrden() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(2, 2);

        Printer printer = farm.reserve(new PrintOrder(7));

        assertEquals(PrinterState.RESERVED, printer.getCurrentState());
        assertEquals(7, printer.getAssignedOrderId());
        assertEquals(1, printer.getUsageCount());
        assertEquals(1, farm.totalOrders());
    }

    @Test
    @DisplayName("release: la impresora vuelve a AVAILABLE, sin orden, y se puede volver a reservar")
    void releaseLaDejaReutilizable() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(1, 1);
        Printer printer = farm.reserve(new PrintOrder(1));

        farm.release(printer);

        assertEquals(PrinterState.AVAILABLE, printer.getCurrentState());
        assertNull(printer.getAssignedOrderId());

        Printer again = farm.reserve(new PrintOrder(2));
        assertSame(printer, again);
        assertEquals(2, again.getAssignedOrderId());
        assertEquals(2, again.getUsageCount());
    }

    @Test
    @DisplayName("setOutOfService: la impresora no se vuelve a entregar (invariante 7)")
    void fueraDeServicioNoSeReutiliza() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(1, 2);
        Printer rota = farm.reserve(new PrintOrder(1));
        Printer sana = farm.reserve(new PrintOrder(2));

        farm.setOutOfService(rota);
        farm.release(sana);

        assertEquals(PrinterState.OUT_OF_SERVICE, rota.getCurrentState());
        assertNull(rota.getAssignedOrderId());

        // Varias reservas seguidas: siempre tiene que salir la sana, nunca la rota
        for (int i = 3; i <= 10; i++) {
            Printer p = farm.reserve(new PrintOrder(i));
            assertSame(sana, p);
            assertNotSame(rota, p);
            farm.release(p);
        }
        assertEquals(PrinterState.OUT_OF_SERVICE, rota.getCurrentState());
    }

    @Test
    @DisplayName("totalOrders suma los usos de todas las impresoras (invariante 8)")
    void totalOrdersCuentaTodasLasAsignaciones() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(2, 2);

        for (int i = 1; i <= 10; i++) {
            Printer p = farm.reserve(new PrintOrder(i));
            farm.release(p);
        }
        Printer rota = farm.reserve(new PrintOrder(11));
        farm.setOutOfService(rota);

        assertEquals(11, farm.totalOrders());
    }

    // ------------------------------------------------------------------ concurrentes

    @Test
    @Timeout(5)
    @DisplayName("Sin impresoras disponibles, reserve bloquea (wait) hasta que alguien hace release")
    void reserveEsperaHastaRelease() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(1, 1);
        Printer unica = farm.reserve(new PrintOrder(1));

        Printer[] obtenida = new Printer[1];
        Thread esperando = new Thread(() -> {
            try {
                obtenida[0] = farm.reserve(new PrintOrder(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "assignment-test");
        esperando.start();

        // El hilo tiene que quedar dormido en wait(), no terminar ni hacer espera activa
        awaitState(esperando, Thread.State.WAITING);
        assertNull(obtenida[0]);

        farm.release(unica);
        esperando.join(2000);

        assertFalse(esperando.isAlive(), "El hilo no se desperto despues del release");
        assertSame(unica, obtenida[0]);
        assertEquals(2, obtenida[0].getAssignedOrderId());
    }

    @Test
    @Timeout(5)
    @DisplayName("setOutOfService no despierta a nadie: la impresora rota no cuenta como disponible")
    void fueraDeServicioNoDespiertaAlQueEspera() throws InterruptedException {
        PrinterFarm farm = new PrinterFarm(1, 2);
        Printer a = farm.reserve(new PrintOrder(1));
        Printer b = farm.reserve(new PrintOrder(2));

        Printer[] obtenida = new Printer[1];
        Thread esperando = new Thread(() -> {
            try {
                obtenida[0] = farm.reserve(new PrintOrder(3));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        esperando.start();
        awaitState(esperando, Thread.State.WAITING);

        farm.setOutOfService(a);              // no libera nada: tiene que seguir esperando
        Thread.sleep(100);
        assertTrue(esperando.isAlive());
        assertNull(obtenida[0]);

        farm.release(b);                      // ahora si
        esperando.join(2000);
        assertSame(b, obtenida[0]);
    }

    @Test
    @Timeout(10)
    @DisplayName("Reservas simultaneas: nunca se entrega la misma impresora a dos hilos (invariante 4)")
    void reservasSimultaneasSonExclusivas() throws InterruptedException {
        int printers = 50;
        PrinterFarm farm = new PrinterFarm(5, 10);
        List<Printer> entregadas = new ArrayList<>();
        List<Thread> hilos = new ArrayList<>();

        for (int i = 1; i <= printers; i++) {
            int orderId = i;
            hilos.add(new Thread(() -> {
                try {
                    Printer p = farm.reserve(new PrintOrder(orderId));
                    synchronized (entregadas) {
                        entregadas.add(p);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        startAndJoin(hilos);

        Set<Printer> distintas = new HashSet<>(entregadas);
        assertEquals(printers, entregadas.size());
        assertEquals(printers, distintas.size(), "Alguna impresora se entrego dos veces");
        for (Printer p : entregadas) {
            assertEquals(PrinterState.RESERVED, p.getCurrentState());
            assertEquals(1, p.getUsageCount());
        }
        assertEquals(printers, farm.totalOrders());
    }

    @Test
    @Timeout(20)
    @DisplayName("Estres reserve/release con mas hilos que impresoras: sin duplicados, sin deadlock y conteo exacto")
    void estresReserveRelease() throws InterruptedException {
        int threads = 16;
        int ciclosPorHilo = 500;
        PrinterFarm farm = new PrinterFarm(2, 2);   // 4 impresoras para 16 hilos: hay espera seguro

        Set<Printer> enUso = new HashSet<>();        // impresoras que algun hilo tiene en este momento
        List<String> errores = new ArrayList<>();
        List<Thread> hilos = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            int base = t * ciclosPorHilo;
            hilos.add(new Thread(() -> {
                try {
                    for (int c = 1; c <= ciclosPorHilo; c++) {
                        PrintOrder order = new PrintOrder(base + c);
                        Printer p = farm.reserve(order);
                        synchronized (enUso) {
                            if (!enUso.add(p)) {
                                errores.add(p.getPrinterId() + " entregada a dos hilos a la vez");
                            }
                        }
                        if (p.getAssignedOrderId() != order.getId()) {
                            synchronized (enUso) {
                                errores.add(p.getPrinterId() + " tiene otra orden asignada");
                            }
                        }
                        synchronized (enUso) {
                            enUso.remove(p);
                        }
                        farm.release(p);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        startAndJoin(hilos);

        assertTrue(errores.isEmpty(), () -> String.join("\n", errores));
        assertEquals(threads * ciclosPorHilo, farm.totalOrders());

        // Al final ninguna queda reservada (invariante 10): se pueden volver a tomar las 4
        Set<Printer> finales = new HashSet<>();
        for (int i = 1; i <= 4; i++) {
            Printer p = farm.reserve(new PrintOrder(100_000 + i));
            finales.add(p);
        }
        assertEquals(4, finales.size());
    }

    // ------------------------------------------------------------------ helpers

    private static void startAndJoin(List<Thread> hilos) throws InterruptedException {
        for (Thread h : hilos) {
            h.start();
        }
        for (Thread h : hilos) {
            h.join(15_000);
            if (h.isAlive()) {
                fail("Un hilo no termino: posible deadlock o hilo que nunca se desperto");
            }
        }
    }

    /** Espera (con limite) a que el hilo llegue al estado indicado. */
    private static void awaitState(Thread thread, Thread.State state) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (thread.getState() != state) {
            if (System.currentTimeMillis() > limite) {
                fail("El hilo no llego a " + state + " (estado actual: " + thread.getState() + ")");
            }
            Thread.sleep(5);
        }
    }
}
