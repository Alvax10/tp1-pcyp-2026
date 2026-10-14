package ar.edu.unc.fcefyn.pcp.tp1.solution;

public class PrinterFarm {
    private final Printer[][] matrix; //Matriz de impresora donde las voy a buscar
    private int availableCount; // cantidad de impresoras disponibles, si es 0 lo mando a dormir

    //Construcctor de la clase fram
    public PrinterFarm(int rows, int columns){
        matrix = new Printer[rows][columns];
        for(int i=0; i<rows ; i++){
            for(int j=0; j<columns; j++){
                matrix[i][j] = new Printer(i, j); //Llamo a constructor de impresora
            }
        }
        availableCount = rows * columns;
    }

    // Como hago para que solo un hilo pueda operar a la vez el avaiableCount ? -> al usar synchronized
    // Soluciono el problema pues solo un hilo puede reservar o liberar el recurso. Con un lock habria que
    // Habria que crear una variable del la cual sea compartida para que solo un hilo pueda tomarlo.

    // Metodo reserve: reserva una impresora disponible para la orden.
    // Si no hay ninguna (availableCount == 0), el hilo se duerme hasta que alguien haga release.
    // Este metodo debe tambien tener en cuenta que solo se puede reservar de a una impresora a la vez
    // por lo cual deberia ser una seccion critica, dentro de ella debe estar el restar uno al  available.
    public synchronized Printer reserve(PrintOrder order) throws InterruptedException {
        while (availableCount == 0) {
            wait(); // soltamos el lock y duerme; al despertar vuelve a chequear la condicion
        }
        Printer printer = findAvailable();
        printer.reserve(order.getId());   // AVAILABLE -> RESERVED, suma usos, guarda la orden
        availableCount--;
        return printer;
    }

    // Metodo release: para liberar una impresora y despertar a los hilos que estan esperando
    // Cuando una impresora termina debe ejecutalo para sumarse uno en available que esa suma es una seccion
    // critica, para que no se sobre escriban datos,
    public synchronized void release(Printer printer) {
        printer.setNewState(AVAILABLE);        // RESERVED -> AVAILABLE, limpia la orden se podria cambiar a un metodo release (Su responsable seria Printer).
        availableCount++;
        notifyAll();
    }

    //Para conseguir la cantidad de ordenes nos conviene crear un metodo que recorra la matriz y obtenga las ordenes de cada una y las sume
    // Habra concurrencia ? -> No siempre que consideremos que totalOders solo se deberia ejecutar al final, cuando totadas las impresora
    // Ya terminaron.
    // Suma los usos de todas las impresoras.
    // PRECONDICION: llamar solo cuando todos los workers terminaron (despues de join()).
    // Por eso no necesita synchronized: join() garantiza la visibilidad de lo escrito.
    public int totalOrders() {
        int totalOrder = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                totalOrder += matrix[i][j].getUsageCount();
            }
        }
        return totalOrder;
    }

    // Busca la primera impresora disponible. Solo se llama desde reserve (ya con el lock tomado).
    private Printer findAvailable() {
        for(int i = 0; i< matrix.length; i++){
            for(int j = 0; j < matrix[i].length; j++){
                Printer printer = matrix[i][j];

                if (printer.isAvailable()) {
                    return printer;
                }
            }
        }
        throw new IllegalStateException("availableCount > 0 pero no se encontro impresora disponible");
    }

}

