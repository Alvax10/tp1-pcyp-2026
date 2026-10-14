package ar.edu.unc.fcefyn.pcp.tp1.solution;
import ar.edu.unc.fcefyn.pcp.tp1.api.OrderState;

public class PrintOrder {
    private final int id;
    private OrderState state = OrderState.CREATED;
    private Printer printer;
    public boolean processing;

    public PrintOrder(int id) {
        this.id = id;
    }

    // menor id primero, porque la lista está ordenada por ID


    public int getId(){
        return this.id;
    }

    public Printer getPrinter(){
        return this.printer;
    }
    // not thread safe
    public OrderState getCurrentState() {
        return this.state;
    }

    // not thread safe
    public void SetState(OrderState state){
        this.state = state;
    }
}