package com.retailpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BusinessEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, name = "`external`")
    private boolean external = false;

    @Column(nullable = false)
    private boolean active = true;

    public BusinessEntity(String name, String location, String type, boolean external) {
        this.name = name;
        this.location = location;
        this.type = type;
        this.external = external;
    }
}
