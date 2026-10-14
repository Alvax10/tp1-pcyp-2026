

------------------ ETAPA 1 ----------------------------------------------

-- FUNCION DE CONCURRENTSIMULATION

public synchronized Execute() {

    -- PrintOrder order = new PrintOrder();

    PrintOrder[500] orders -- Array de orders

    THREAD new thread
    THREAD new thread
    THREAD new thread

    thread.start();
    thread.start();
    thread.start();

    PrintOrder availableOrder = orders.getOrderInState(CREATED); -- created = orderState -- AGARRA UN PRINTORDER LIBRE
    Printer currentPrinter = farm.getPrinter(availableOrder.getId()); -- printerState = available -- AGARRA LA IMPRESORA LIBRE
    currentPrinter.reserve(availableOrder.getId()); -- RESERVA LA IMPRESORA;

    availableOrder.setNewState(WAITING_VALIDATION);
    availableOrder.printer = currentPrinter.id;
}

-- FUNCION DE FARM

reservePrinter(orderId) {
    for() {
        isAvailable ? return printer.reserve(orderId) : break;
    }
}

------------------- ETAPA 2 ---------------------------------------

        thread.start();
        thread.start();

        -- esto sigue en execute
        PrinteOrder waitingOrder = orders.getOrderInState(WAITING_VALIDATION);
        OutcomeDecider.isModelValid(waitingOrder.getId());

        -- SI EL MODELO ES VALIDO
                waitingOrder.setNewState(READY_TO_PRINT);

        -- SI EL MODELO FALLA
                waitingOrder.setNewState(REJECTED);
                waitingOrder.printer.setAvailable();

-------------- ETAPA 3 (IMPRESION) ----------------------------------------

        thread.start();
        thread.start();
        thread.start();

        -- esto sigue en execute
        PrinteOrder readyOrder = orders.getOrderInState(READY_TO_PRINT);
        OutcomeDecider.isPrintSuccessful(readyOrder.getId());

        -- SI LA IMPRESION ES VALIDA
                readyOrder.setNewState(PRINTED);
                readyOrder.printer.setAvailable();

        -- SI LA IMPRESION FALLA
                readyOrder.setNewState(PRINT_FAILED);
                readyOrder.printer.setOutOfService();

-------------- ETAPA 4 (CONTROL DE CALIDAD) ----------------------------------------

        THREAD new thread
        THREAD new thread

        thread.start();
        thread.start();

        -- esto sigue en execute
        PrinteOrder printedOrder = orders.getOrderInState(PRINTED);
        if OutcomeDecider.isQualityApproved(printedOrder.getId());

        -- SI LA CALIDAD ES SUFICIENTE
                printedOrder.setNewState(APPROVED);

        -- SI LA CALIDAD NO ES SUFICIENTE
                printedOrder.setNewState(DEFECTIVE);

        evenctoLog.decreaseOrders()


---------------------- DETERMINACION DE RESULTADOS -----------------------

        -- Esto lo decide OutcomeDecider







