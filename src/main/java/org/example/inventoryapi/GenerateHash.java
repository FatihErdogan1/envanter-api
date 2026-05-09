package org.example.inventoryapi;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "admin123";
        System.out.println(new BCryptPasswordEncoder().encode(password));
    }
}
