package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import org.example.inventoryapi.model.enums.TransactionType;
import java.time.LocalDateTime;

@Entity
@Table(name = "Inventory_Transactions")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "notes")
    private String notes;

    public InventoryTransaction() {}

    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }
    public Product getProduct()                         { return product; }
    public void setProduct(Product p)                   { this.product = p; }
    public Warehouse getWarehouse()                     { return warehouse; }
    public void setWarehouse(Warehouse w)               { this.warehouse = w; }
    public Warehouse getDestinationWarehouse()          { return destinationWarehouse; }
    public void setDestinationWarehouse(Warehouse w)    { this.destinationWarehouse = w; }
    public User getUser()                               { return user; }
    public void setUser(User u)                         { this.user = u; }
    public TransactionType getType()                    { return type; }
    public void setType(TransactionType t)              { this.type = t; }
    public int getQuantity()                            { return quantity; }
    public void setQuantity(int qty)                    { this.quantity = qty; }
    public LocalDateTime getTransactionDate()           { return transactionDate; }
    public void setTransactionDate(LocalDateTime d)     { this.transactionDate = d; }
    public String getNotes()                            { return notes; }
    public void setNotes(String notes)                  { this.notes = notes; }
}
