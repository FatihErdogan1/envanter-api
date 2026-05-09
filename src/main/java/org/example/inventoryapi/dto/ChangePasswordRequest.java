package org.example.inventoryapi.dto;

public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String p) { this.currentPassword = p; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String p) { this.newPassword = p; }
}
