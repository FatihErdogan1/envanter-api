package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Asset_Transfers")
public class AssetTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private int id;

    @Column(name = "asset_id", nullable = false)
    private int assetId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    private Warehouse toWarehouse;

    @Column(name = "transfer_date")
    private LocalDateTime transferDate;

    @Column(name = "notes")
    private String notes;

    public AssetTransfer() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public int getAssetId()                         { return assetId; }
    public void setAssetId(int assetId)             { this.assetId = assetId; }
    public Warehouse getFromWarehouse()             { return fromWarehouse; }
    public void setFromWarehouse(Warehouse w)       { this.fromWarehouse = w; }
    public Warehouse getToWarehouse()               { return toWarehouse; }
    public void setToWarehouse(Warehouse w)         { this.toWarehouse = w; }
    public LocalDateTime getTransferDate()          { return transferDate; }
    public void setTransferDate(LocalDateTime d)    { this.transferDate = d; }
    public String getNotes()                        { return notes; }
    public void setNotes(String notes)              { this.notes = notes; }
}
