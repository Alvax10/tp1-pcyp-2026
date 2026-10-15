package ar.edu.unc.fcefyn.pcp.tp1.solution;

/**
 * Hilo de una etapa. Cada subclase implementa process(), que procesa UNA orden:
 *   1. tomar una orden del estado de entrada (bloqueante, en el monitor de OrderSource)
 *   2. simulateWork(): demora de la etapa, FUERA de cualquier lock
 *   3. decidir y confirmar la transicion (de nuevo dentro de los monitores)
 * y devuelve false cuando ya no queda trabajo (OrderSource devolvio null).
 */
public abstract class Worker implements Runnable {

    private final StageState stage;
    private final String name;
    private final long delayMs;

    protected Worker(StageState stage, String name, long delayMs) {
        if (stage == null) {
            throw new IllegalArgumentException(name + ": stage no puede ser null");
        }
        this.stage = stage;
        this.name = name;
        this.delayMs = delayMs;
    }

    /** Procesa una orden. Devuelve false cuando todas las ordenes son terminales. */
    protected abstract boolean process() throws InterruptedException;

    @Override
    public void run() {
        try {
            while (process()) {
                // una orden por vuelta; la demora se aplica dentro de process()
            }
        } catch (InterruptedException e) {
            // Nadie interrumpe a los workers en una ejecucion normal; si pasa, se preserva el flag y se sale
            Thread.currentThread().interrupt();
        }
    }

    /** Demora de la etapa: una vez por orden efectivamente procesada, sin tener ningun lock. */
    protected void simulateWork() throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }

    public StageState getStage() { return stage; }
    public String getName() { return name; }
    public long getDelayMs() { return delayMs; }
}
