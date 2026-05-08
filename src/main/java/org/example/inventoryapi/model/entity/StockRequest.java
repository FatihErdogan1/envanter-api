package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import org.example.inventoryapi.model.enums.StockRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "Stock_Requests")
public class StockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockRequestStatus status;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "manager_note")
    private String managerNote;

    public StockRequest() {}

    public int getId()                                      { return id; }
    public void setId(int id)                               { this.id = id; }
    public Product getProduct()                             { return product; }
    public void setProduct(Product product)                 { this.product = product; }
    public Warehouse getWarehouse()                         { return warehouse; }
    public void setWarehouse(Warehouse warehouse)           { this.warehouse = warehouse; }
    public User getRequestedBy()                            { return requestedBy; }
    public void setRequestedBy(User requestedBy)            { this.requestedBy = requestedBy; }
    public User getReviewedBy()                             { return reviewedBy; }
    public void setReviewedBy(User reviewedBy)              { this.reviewedBy = reviewedBy; }
    public int getQuantity()                                { return quantity; }
    public void setQuantity(int quantity)                   { this.quantity = quantity; }
    public StockRequestStatus getStatus()                   { return status; }
    public void setStatus(StockRequestStatus status)        { this.status = status; }
    public LocalDateTime getRequestDate()                   { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate)   { this.requestDate = requestDate; }
    public LocalDateTime getReviewDate()                    { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate)     { this.reviewDate = reviewDate; }
    public String getNotes()                                { return notes; }
    public void setNotes(String notes)                      { this.notes = notes; }
    public String getManagerNote()                          { return managerNote; }
    public void setManagerNote(String managerNote)          { this.managerNote = managerNote; }
}
