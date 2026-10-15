package ar.edu.unc.fcefyn.pcp.tp1.solution;

public class TerminationTracker {

    private final int totalOrders;
    private int terminalCount;

    public TerminationTracker(int totalOrders) {
        if (totalOrders < 0) {
            throw new IllegalArgumentException("totalOrders no puede ser negativo");
        }
        this.totalOrders = totalOrders;
        this.terminalCount = 0;
    }

    public synchronized void orderFinished() {
        if (terminalCount >= totalOrders) {
            throw new IllegalStateException("Más órdenes terminales que totales");
        }
        terminalCount++;
        if (terminalCount == totalOrders) {
            this.notifyAll();
        }
    }

    public synchronized void awaitAll() throws InterruptedException {
        while (terminalCount < totalOrders) {
            this.wait();
        }
    }
}