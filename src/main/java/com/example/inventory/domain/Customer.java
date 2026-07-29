package com.example.inventory.domain;

public class Customer {
    private final String id;
    private final String name;
    private final String email;
    private final Address address;

    public Customer(String id, String name, String email, Address address) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }
}
