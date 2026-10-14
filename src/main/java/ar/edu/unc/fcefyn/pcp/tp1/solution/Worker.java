package ar.edu.unc.fcefyn.pcp.tp1.solution;

public abstract class Worker implements Runnable {

    private final Stage stage;
    private final String name;
    private final long delayMs;

    protected Worker(Stage stage, String name, long delayMs) {
        this.stage = stage;
        this.name = name;
        this.delayMs = delayMs;
    }

    /** Workers of each stage (implemented by sub-classes). */
    protected abstract void process() throws InterruptedException;

    @Override
    public void run() {

        System.out.println("Thread: " + this.name + " initiated \n");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                inspect();
                process();
                Thread.sleep(delayMs); // simula el tiempo de la etapa
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } finally {
            System.out.println("Thread: " + this.name + " terminated \n");
        }
    }

    /** Internal Check previous to each iteration (state/stage del worker). */
    private void inspect() {
        if (stage == null) {
            throw new IllegalStateException(name + " no stage assigned \n");
        }
        log("Inspecting stage: \n");
    }

    protected void log(String msg) {
        System.out.println("[" + stage + "][" + name + "] " + msg + "\n");
    }

    public Stage getStage()  { return this.stage; }
    public String getName()  { return this.name; }
    public long getDelayMs() { return this.delayMs; }
}