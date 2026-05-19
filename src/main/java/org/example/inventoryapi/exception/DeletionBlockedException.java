package org.example.inventoryapi.exception;

public class DeletionBlockedException extends RuntimeException {

    private final int    count;
    private final String relatedEntity;

    public DeletionBlockedException(String reason, int count, String relatedEntity) {
        super(reason);
        this.count         = count;
        this.relatedEntity = relatedEntity;
    }

    public int    getCount()         { return count; }
    public String getRelatedEntity() { return relatedEntity; }
}
