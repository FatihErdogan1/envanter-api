package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import org.example.inventoryapi.model.enums.SupplierOrderStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "Supplier_Orders")
public class SupplierOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupplierOrderStatus status;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    public SupplierOrder() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public Product getProduct()                     { return product; }
    public void setProduct(Product p)               { this.product = p; }
    public Supplier getSupplier()                   { return supplier; }
    public void setSupplier(Supplier s)             { this.supplier = s; }
    public Warehouse getWarehouse()                 { return warehouse; }
    public void setWarehouse(Warehouse w)           { this.warehouse = w; }
    public User getCreatedBy()                      { return createdBy; }
    public void setCreatedBy(User u)                { this.createdBy = u; }
    public int getQuantity()                        { return quantity; }
    public void setQuantity(int q)                  { this.quantity = q; }
    public SupplierOrderStatus getStatus()          { return status; }
    public void setStatus(SupplierOrderStatus s)    { this.status = s; }
    public LocalDateTime getOrderDate()             { return orderDate; }
    public void setOrderDate(LocalDateTime d)       { this.orderDate = d; }
    public LocalDateTime getUpdatedDate()           { return updatedDate; }
    public void setUpdatedDate(LocalDateTime d)     { this.updatedDate = d; }
}
