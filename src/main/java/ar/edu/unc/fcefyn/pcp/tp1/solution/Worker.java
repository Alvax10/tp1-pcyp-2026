package ar.edu.unc.fcefyn.pcp.tp1.solution;

/**
 * Clase abstracta base de los workers del pipeline.
 */
public abstract class Worker implements Runnable {

    private final Stage stage;
    private final String name;
    private final long delayMs;

    protected Worker(Stage stage, String name, long delayMs) {
        this.stage = stage;
        this.name = name;
        this.delayMs = delayMs;
    }

    /** Trabajo específico de cada etapa (lo implementa cada subclase). */
    protected abstract void process() throws InterruptedException;

    @Override
    public void run() {

        System.out.println("Hilo: " + this.name + " iniciado \n");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                inspect();
                process();
                Thread.sleep(delayMs); // simula el tiempo de la etapa
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } finally {
            System.out.println("Hilo: " + this.name + " finalizado \n");
        }
    }

    /** Chequeo interno previo a cada iteración (estado/etapa del worker). */
    private void inspect() {
        if (stage == null) {
            throw new IllegalStateException(name + " no tiene etapa asignada");
        }
        log("Inspeccionando Etapa ");
    }

    protected void log(String msg) {
        System.out.println("[" + stage + "][" + name + "] " + msg);
    }

    public Stage getStage()  { return this.stage; }
    public String getName()  { return this.name; }
    public long getDelayMs() { return this.delayMs; }
}