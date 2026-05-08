package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Asset_Assignments")
public class AssetAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private int id;

    @Column(name = "asset_id", nullable = false)
    private int assetId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "notes")
    private String notes;

    public AssetAssignment() {}

    public int getId()                                      { return id; }
    public void setId(int id)                               { this.id = id; }
    public int getAssetId()                                 { return assetId; }
    public void setAssetId(int assetId)                     { this.assetId = assetId; }
    public User getUser()                                   { return user; }
    public void setUser(User user)                          { this.user = user; }
    public LocalDateTime getAssignedDate()                  { return assignedDate; }
    public void setAssignedDate(LocalDateTime d)            { this.assignedDate = d; }
    public LocalDateTime getReturnDate()                    { return returnDate; }
    public void setReturnDate(LocalDateTime d)              { this.returnDate = d; }
    public String getNotes()                                { return notes; }
    public void setNotes(String notes)                      { this.notes = notes; }
    public boolean isActive()                               { return returnDate == null; }
}
