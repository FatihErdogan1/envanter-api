package org.example.inventoryapi.dto;

public record DeletionErrorResponse(String error, String reason, int count, String relatedEntity) {

    public static DeletionErrorResponse of(String reason, int count, String relatedEntity) {
        return new DeletionErrorResponse("DELETION_BLOCKED", reason, count, relatedEntity);
    }
}
