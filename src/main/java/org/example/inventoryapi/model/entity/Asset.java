package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import org.example.inventoryapi.model.enums.AssetStatus;
import java.time.LocalDate;

@Entity
@Table(name = "Assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssetStatus status;

    // DB'de saklanmaz; sorgu ile doldurulur
    @Transient
    private String assignedToUsername;

    public Asset() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public Supplier getSupplier()                   { return supplier; }
    public void setSupplier(Supplier s)             { this.supplier = s; }
    public Warehouse getWarehouse()                 { return warehouse; }
    public void setWarehouse(Warehouse w)           { this.warehouse = w; }
    public String getSerialNumber()                 { return serialNumber; }
    public void setSerialNumber(String sn)          { this.serialNumber = sn; }
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    public LocalDate getPurchaseDate()              { return purchaseDate; }
    public void setPurchaseDate(LocalDate d)        { this.purchaseDate = d; }
    public AssetStatus getStatus()                  { return status; }
    public void setStatus(AssetStatus status)       { this.status = status; }
    public String getAssignedToUsername()           { return assignedToUsername; }
    public void setAssignedToUsername(String u)     { this.assignedToUsername = u; }
}
