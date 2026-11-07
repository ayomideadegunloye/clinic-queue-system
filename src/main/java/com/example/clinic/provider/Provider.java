package com.example.clinic.provider;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "provider")
@Getter @Setter @NoArgsConstructor
public class Provider {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(name = "service_label", nullable = false)
    private String serviceLabel;
    private boolean active = true;
}
