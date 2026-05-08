package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Asset_Maintenance")
public class AssetMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private int id;

    @Column(name = "asset_id", nullable = false)
    private int assetId;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "description")
    private String description;

    @Column(name = "notes")
    private String notes;

    public AssetMaintenance() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public int getAssetId()                         { return assetId; }
    public void setAssetId(int assetId)             { this.assetId = assetId; }
    public LocalDateTime getStartDate()             { return startDate; }
    public void setStartDate(LocalDateTime d)       { this.startDate = d; }
    public LocalDateTime getEndDate()               { return endDate; }
    public void setEndDate(LocalDateTime d)         { this.endDate = d; }
    public String getDescription()                  { return description; }
    public void setDescription(String desc)         { this.description = desc; }
    public String getNotes()                        { return notes; }
    public void setNotes(String notes)              { this.notes = notes; }
    public boolean isActive()                       { return endDate == null; }
}
