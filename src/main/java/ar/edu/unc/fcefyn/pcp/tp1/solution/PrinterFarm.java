package ar.edu.unc.fcefyn.pcp.tp1.solution;

public class PrinterFarm {
    private final Printer[][] matrix; //Matriz de impresora donde las voy a buscar
    private int availableCount; // cantidad de impresoras disponibles, si es 0 lo mando a dormir

    //Construcctor de la clase fram
    public PrinterFarm(int filas, int columnas){
        matrix = new Printer[filas][columnas];
        for(int i=0; i<filas ; i++){
            for(int j=0; j<columnas; j++){
                matrix[i][j] = new Printer(); //Definir constructor de impresora
            }
        }
        availableCount = filas * columnas;
    }

    // Como hago para que solo un hilo pueda operar a la vez el avaiableCount ? -> al usar synchronized
    // Soluciono el problema pues solo un hilo puede reservar o liberar el recurso. Con un lock habria que
    // Habria que crear una variable del la cual sea compartida para que solo un hilo pueda tomarlo.

    // Metodo reserve para reservar una impresora, en caso que sea available = 0 entonces me duermo!
    // Este metodo debe tambien tener en cuenta que solo se puede reservar de a una impresora a la vez
    // por lo cual deberia ser una seccion critica, dentro de ella debe estar el restar uno al  available.

    // Metodo reserve: reserva una impresora disponible para la orden.
    // Si no hay ninguna (availableCount == 0), el hilo se duerme hasta que alguien haga release.
    //  Todo el metodo es seccion critica: buscar + reservar + restar tiene que ser atomico.
    public synchronized Printer reserve(Order order) throws InterruptedException {
        while (availableCount == 0) {
            wait(); // soltamos el lock y duerme; al despertar vuelve a chequear la condicion
        }
        Printer printer = findAvailable();
        printer.reserve(order);   // AVAILABLE -> RESERVED, suma usos, guarda la orden
        availableCount--;
        return printer;
    }

    // Metodo realese para liberar una impresora y despertar a los hilos que estan esperando
    // Cuando una impresora termina debe ejecutalo para sumarse uno en available que esa suma es una seccion
    // critica, para que no se sobre escriban datos,

    // Metodo release: lo llama el worker que termino de usar la impresora.
    // Suma uno a availableCount y despierta a los hilos que esperan en reserve.
    public synchronized void release(Printer printer) {
        printer.release();        // RESERVED -> AVAILABLE, limpia la orden
        availableCount++;
        notifyAll();
    }

    // Busca la primera impresora disponible. Solo se llama desde reserve (ya con el lock tomado).
    private Printer findAvailable() {
        for (Printer[] fila : matrix) {
            for (Printer printer : fila) {
                if (printer.getCurrentState() == PrinterState.AVAILABLE) {
                    return printer;
                }
            }
        }
        throw new IllegalStateException("availableCount > 0 pero no se encontro impresora disponible");
    }

}

