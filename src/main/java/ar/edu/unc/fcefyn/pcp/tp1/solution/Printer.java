package ar.edu.unc.fcefyn.pcp.tp1.solution;

import ar.edu.unc.fcefyn.pcp.tp1.api.PrinterState;

public class Printer {
    private final String printerId;
    private PrinterState currentState;
    private int usageCount;
    private Integer assignedOrderId;

    public Printer(int row, int col) {
        printerId = "P-"+row+"-"+col;
        currentState = PrinterState.AVAILABLE;
        usageCount = 0;
        assignedOrderId = null;
    }

    private void setAssignedOrderId(int orderId) {
        assignedOrderId = orderId;
    }

    private void incrementUsageCount() {
        usageCount++;
    }

    public void reserve(int orderId) {
        setState(PrinterState.RESERVED);
        incrementUsageCount();
        setAssignedOrderId(orderId);
    }

    public void setState(PrinterState state) {
        currentState = state;
        assignedOrderId = null;
    }
    
    public boolean isAvailable() {
        return currentState == PrinterState.AVAILABLE;
    }

    public String getPrinterId() {
        return printerId;
    }

    public PrinterState getCurrentState() {
        return currentState;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public Integer getAssignedOrderId() {
        return assignedOrderId;
    }
}
