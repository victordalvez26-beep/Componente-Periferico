package uy.edu.tse.hcen.rest;

/**
 * Simple wrapper: payload must be a JSON string containing the clinic data.
 * We accept it as a string to avoid coupling to a specific model here.
 */
public class AltaClinicaRequest {
    public String payload;

    public AltaClinicaRequest() {}
}
