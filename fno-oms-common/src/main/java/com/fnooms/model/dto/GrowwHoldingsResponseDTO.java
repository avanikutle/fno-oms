package com.fnooms.model.dto;

import java.util.List;

public class GrowwHoldingsResponseDTO {
    private String status;
    private Payload payload;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Payload getPayload() { return payload; }
    public void setPayload(Payload payload) { this.payload = payload; }

    public static class Payload {
        private List<GrowwHoldingDTO> holdings;

        public List<GrowwHoldingDTO> getHoldings() { return holdings; }
        public void setHoldings(List<GrowwHoldingDTO> holdings) { this.holdings = holdings; }
    }
}
