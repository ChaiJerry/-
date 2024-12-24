package com.bundling.dto;

public class TrainDTO {
    private String ticket_data_id;
    private String meal_data_id;
    private String baggage_data_id;
    private String insurance_data_id;
    private String seat_data_id;
    private String minSupport;
    private String minConfidence;
    private String comment;

    // Getters and setters
    public String getTicket_data_id() {
        return ticket_data_id;
    }

    public void setTicket_data_id(String ticket_data_id) {
        this.ticket_data_id = ticket_data_id;
    }

    public String getMeal_data_id() {
        return meal_data_id;
    }

    public void setMeal_data_id(String meal_data_id) {
        this.meal_data_id = meal_data_id;
    }

    public String getBaggage_data_id() {
        return baggage_data_id;
    }

    public void setBaggage_data_id(String baggage_data_id) {
        this.baggage_data_id = baggage_data_id;
    }

    public String getInsurance_data_id() {
        return insurance_data_id;
    }

    public void setInsurance_data_id(String insurance_data_id) {
        this.insurance_data_id = insurance_data_id;
    }

    public String getSeat_data_id() {
        return seat_data_id;
    }

    public void setSeat_data_id(String seat_data_id) {
        this.seat_data_id = seat_data_id;
    }

    public String getMinSupport() {
        return minSupport;
    }

    public void setMinSupport(String minSupport) {
        this.minSupport = minSupport;
    }

    public String getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(String minConfidence) {
        this.minConfidence = minConfidence;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}