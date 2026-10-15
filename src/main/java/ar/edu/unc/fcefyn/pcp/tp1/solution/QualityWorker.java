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
 * Sincronizacion: este worker no tiene estado compartido propio. Todo el acceso concurrente
 * a las ordenes pasa por OrderSource (monitor LOCK):
 *   - getOrderInState(PRINTED) marca la orden como "processing" dentro del lock, por lo que
 *     dos QualityWorker no pueden auditar la misma orden (invariante 3).
 *   - Si no hay ordenes PRINTED, el hilo queda en LOCK.wait() (sin espera activa) hasta que
 *     un PrintingWorker haga SetOrderState(..., PRINTED) y llame a notifyAll().
 *   - Cuando todas las ordenes son terminales, getOrderInState devuelve null y el worker termina.
 */
public class QualityWorker extends Worker {

    private final OrderSource orderSource; // de donde toma las ordenes PRINTED
    private final SimulationConfig config; // la usa OutcomeDecider (semilla y porcentaje)

    QualityWorker(StageState stage, String name, long delayMs,
                  OrderSource orderSource, SimulationConfig config) {
        super(stage, name, delayMs);
        this.orderSource = orderSource;
        this.config = config;
    }

    @Override
    protected boolean process() throws InterruptedException {
        //Tomo una orden PRINTED (bloqueante: si no hay, el hilo duerme en el monitor de OrderSource)
        PrintOrder printedOrder = orderSource.getOrderInState(OrderState.PRINTED);
        //null = todas las ordenes ya son terminales: no queda trabajo para esta etapa
        if (printedOrder == null) {
            return false;
        }

        //Tiempo del control de calidad, fuera de cualquier lock
        simulateWork();

        //Decision determinista (no toca estado compartido) y transicion dentro de OrderSource
        if (OutcomeDecider.isQualityApproved(printedOrder.getId(), config)) {
            orderSource.SetOrderState(printedOrder, OrderState.APPROVED, this);  // PRINTED -> APPROVED
        } else {
            orderSource.SetOrderState(printedOrder, OrderState.DEFECTIVE, this); // PRINTED -> DEFECTIVE
        }
        //No hay que liberar impresora: ya se libero en la etapa de impresion
        return true;
    }
}
