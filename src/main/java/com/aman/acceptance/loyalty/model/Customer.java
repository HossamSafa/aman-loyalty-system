package com.aman.acceptance.loyalty.model;

import com.aman.acceptance.loyalty.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // Customer.builder().mobileNumber("+201012345678").name("Ahmed").build();
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_hash", nullable = false, unique = true, length = 64)
    private String mobileHash;

    @Column(name = "mobile_encrypted", nullable = false, length = 255)
    private String mobileEncrypted;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        if(this.status == null) {
            this.status = CustomerStatus.ACTIVE;
        }
    }

}
