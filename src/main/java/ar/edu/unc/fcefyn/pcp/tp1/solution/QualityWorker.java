package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;
import ar.edu.unc.fcefyn.pcp.tp1.api.OutcomeDecider;
import ar.edu.unc.fcefyn.pcp.tp1.api.SimulationConfig;

/** --------------------------- STAGE 4 - QUALITY CONTROL ------------------------------ **/

/*
 * Toma ordenes en PRINTED y decide, con OutcomeDecider.isQualityApproved:
 *     PRINTED -> APPROVED
 *     PRINTED -> DEFECTIVE
 *
 * En esta etapa NO se usan impresoras: la impresora ya fue liberada en PrintingWorker.
 * Ambos resultados son estados terminales, asi que cada orden que pasa por aca termina.
 *
 * Sincronizacion: este worker no tiene estado compartido propio.
 *     el acceso concu a las ordenes pasa por OrderSource (monitor LOCK):
 *   - getOrderInState(PRINTED) marca la orden como "processing" dentro del lock, por lo que
 *     dos QualityWorker no deben pueder auditar la misma orden (invariante 3).
 *   - Si no hay ordenes PRINTED, el hilo queda en LOCK.wait() (sin espera activa) hasta que
 *     un PrintingWorker haga SetOrderState(..., PRINTED) y llame a notifyAll().
 *   - Cuando todas las ordenes son terminales, getOrderInState devuelve null y el worker termina.
 */
public class QualityWorker extends Worker {

    private final OrderSource orderSource; // de donde toma las ordenes PRINTED
    private final SimulationConfig config; // la usa OutcomeDecider (semilla y porcentaje)

    public QualityWorker(StageState stage, String name, long delayMs,
                         OrderSource orderSource, SimulationConfig config) {
        super(stage, name, delayMs);
        this.orderSource = orderSource;
        this.config = config;
    }

    @Override
    protected void process() throws InterruptedException {

        //Tomo una orden PRINTED (bloqueante: si no hay, el hilo duerme en el monitor de OrderSource)
        PrintOrder printedOrder = orderSource.getOrderInState(OrderState.PRINTED);

        // si es null entonces todas las ordenes ya son terminales: no queda trabajo para esta etapa
        if (printedOrder == null) {
            // Marcamos fin del worker: el while de Worker.run() corta en la proxima vuelta
            // (y el Thread.sleep posterior lanza InterruptedException enseguida, sin demorar).
            Thread.currentThread().interrupt();
            return;
        }

        //Transicion de estado DENTRO de OrderSource (lock + notifyAll + conteo de terminales)
        if (isApproved(printedOrder)) {
            orderSource.SetOrderState(printedOrder, OrderState.APPROVED, getName());  // PRINTED -> APPROVED
        } else {
            orderSource.SetOrderState(printedOrder, OrderState.DEFECTIVE, getName()); // PRINTED -> DEFECTIVE
        }

        // La demora (qualityControlDelayMillis) la aplica Worker.run() una vez por orden procesada.
        // No hay que liberar impresora: ya se libero en la etapa de impresion.
    }
    //Decision determinista (fuera del lock: no toca estado compartido)
    private boolean isApproved(PrintOrder order) {
        return OutcomeDecider.isQualityApproved(order.getId(), config);
    }
}