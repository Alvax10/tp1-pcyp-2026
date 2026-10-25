package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

public class PrintOrder {
    private final int id;
    private OrderState state = OrderState.CREATED;
    private Printer printer;
    public boolean processing;
    // cuantas veces paso esta orden por cada etapa (para elementos.csv). Solo se tocan con el lock de OrderSource
    int assignmentCount, validationCount, printingCount, qualityControlCount = 0;

    public PrintOrder(int id) { // constructor
        this.id = id;
    }

    // menor id primero, porque la lista estÃ¡ ordenada por ID


    public int getId(){
        return this.id;
    }

    public Printer getPrinter(){
        return this.printer;
    }

    public void setPrinter(Printer pr){
        this.printer = pr;
    }

    // not thread safe
    public OrderState getCurrentState() {
        return this.state;
    }

    // not thread safe
    public void SetState(OrderState state){
        this.state = state;
    }

    public int getAssignmentCount(){ return assignmentCount; }
    public int getValidationCount(){ return validationCount; }
    public int getPrintingCount(){ return printingCount; }
    public int getQualityControlCount(){ return qualityControlCount; }

    public void incrementStageCount() {

        switch (getCurrentState()) {

            case OrderState.WAITING_VALIDATION -> this.assignmentCount ++;
            case OrderState.READY_TO_PRINT -> this.validationCount ++;
            case OrderState.PRINTED -> this.printingCount ++;
            case OrderState.APPROVED -> this.qualityControlCount ++;
            default -> {}
        }
    }
}