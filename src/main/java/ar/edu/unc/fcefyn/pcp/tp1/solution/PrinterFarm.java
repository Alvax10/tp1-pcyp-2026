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

    // Como hago para que solo un hilo pueda operar a la vez el avaiableCount ?

    // Metodo realese para liberar una impresora y despertar a los hilos que estan esperando
    // Cuando una impresora termina debe ejecutalo para sumarse uno en available que esa suma es una seccion
    // critica, para que no se sobre escriban datos,


    // Metodo reserve para reservar una impresora, en caso que sea available = 0 entonces me duermo!
    // Este metodo debe tambien tener en cuenta que solo se puede reservar de a una impresora a la vez
    // por lo cual deberia ser una seccion critica, dentro de ella debe estar el restar uno al  available.


}

