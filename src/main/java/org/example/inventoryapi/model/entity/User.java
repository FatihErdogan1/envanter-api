package org.example.inventoryapi.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.entity.Warehouse;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email", unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    public User() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }
    public String getPasswordHash()             { return passwordHash; }
    public void setPasswordHash(String hash)    { this.passwordHash = hash; }
    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }
    public Role getRole()                       { return role; }
    public void setRole(Role role)              { this.role = role; }
    public boolean isActive()                   { return isActive; }
    public void setActive(boolean active)       { this.isActive = active; }
    public boolean isForcePasswordChange()      { return forcePasswordChange; }
    public void setForcePasswordChange(boolean v) { this.forcePasswordChange = v; }
    public Warehouse getWarehouse()               { return warehouse; }
    public void setWarehouse(Warehouse w)         { this.warehouse = w; }
}
