package com.fnooms.algo;

public class TradeState {
    private boolean isEntered = false;
    private boolean isExited = false;
    private double currentStopLoss = 0.0;
    private double entryPrice = 0.0;
    private double currentTarget = 0.0;
    
    private String entryOrderId;
    private String exitOrderId;
    private int entryAttempts = 0;

    public double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public double getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(double currentTarget) {
        this.currentTarget = currentTarget;
    }

    public int getEntryAttempts() {
        return entryAttempts;
    }

    public void setEntryAttempts(int entryAttempts) {
        this.entryAttempts = entryAttempts;
    }

    public boolean isEntered() {
        return isEntered;
    }

    public void setEntered(boolean entered) {
        isEntered = entered;
    }

    public boolean isExited() {
        return isExited;
    }

    public void setExited(boolean exited) {
        isExited = exited;
    }

    public double getCurrentStopLoss() {
        return currentStopLoss;
    }

    public void setCurrentStopLoss(double currentStopLoss) {
        this.currentStopLoss = currentStopLoss;
    }

    public String getEntryOrderId() {
        return entryOrderId;
    }

    public void setEntryOrderId(String entryOrderId) {
        this.entryOrderId = entryOrderId;
    }

    public String getExitOrderId() {
        return exitOrderId;
    }

    public void setExitOrderId(String exitOrderId) {
        this.exitOrderId = exitOrderId;
    }
}
