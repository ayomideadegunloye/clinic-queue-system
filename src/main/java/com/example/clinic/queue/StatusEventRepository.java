package com.example.clinic.queue;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusEventRepository extends JpaRepository<StatusEvent, UUID> {}
