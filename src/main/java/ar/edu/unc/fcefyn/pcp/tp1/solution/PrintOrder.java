package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

/**
 * Orden de impresion.
 *
 * NO es thread-safe por si sola: todos sus campos mutables (state, processing y contadores)
 * se leen y escriben SOLO con el lock de OrderSource tomado. El campo printer lo escribe el
 * AssignWorker antes de publicar la orden con SetOrderState (que toma el lock), asi que los
 * demas hilos lo ven al tomar ese mismo lock (happens-before del monitor).
 */
public class PrintOrder {
    private final int id;
    private OrderState state = OrderState.CREATED;
    private Printer printer;            // impresora usada (se conserva como dato historico)
    private boolean processing;         // true mientras un worker la tiene tomada

    // Cuantas veces paso esta orden por cada etapa (para elementos.csv / OrderSnapshot)
    private int assignmentCount;
    private int validationCount;
    private int printingCount;
    private int qualityControlCount;

    public PrintOrder(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    public Printer getPrinter() { return printer; }
    public void setPrinter(Printer printer) { this.printer = printer; }

    /** Id historico de la impresora, o null si nunca se le asigno una. */
    public String getPrinterId() { return printer == null ? null : printer.getPrinterId(); }

    public OrderState getCurrentState() { return state; }
    public void setState(OrderState state) { this.state = state; }

    public boolean isProcessing() { return processing; }
    public void setProcessing(boolean processing) { this.processing = processing; }

    /** Suma una pasada por la etapa que procesa ordenes en el estado dado. */
    public void countStage(OrderState takenFrom) {
        switch (takenFrom) {
            case CREATED -> assignmentCount++;
            case WAITING_VALIDATION -> validationCount++;
            case READY_TO_PRINT -> printingCount++;
            case PRINTED -> qualityControlCount++;
            default -> throw new IllegalArgumentException("Ninguna etapa toma ordenes en " + takenFrom);
        }
    }

    public int getAssignmentCount() { return assignmentCount; }
    public int getValidationCount() { return validationCount; }
    public int getPrintingCount() { return printingCount; }
    public int getQualityControlCount() { return qualityControlCount; }
}
