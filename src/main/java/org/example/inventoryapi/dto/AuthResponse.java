package org.example.inventoryapi.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private boolean forcePasswordChange;
    private Integer warehouseId;
    private String warehouseName;

    public AuthResponse(String token, String username, String role, boolean forcePasswordChange,
                        Integer warehouseId, String warehouseName) {
        this.token               = token;
        this.username            = username;
        this.role                = role;
        this.forcePasswordChange = forcePasswordChange;
        this.warehouseId         = warehouseId;
        this.warehouseName       = warehouseName;
    }

    public String getToken()                { return token; }
    public String getUsername()             { return username; }
    public String getRole()                 { return role; }
    public boolean isForcePasswordChange()  { return forcePasswordChange; }
    public Integer getWarehouseId()         { return warehouseId; }
    public String getWarehouseName()        { return warehouseName; }
}
