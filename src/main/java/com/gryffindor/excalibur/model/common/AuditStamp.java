package com.gryffindor.excalibur.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
@Data
public abstract class AuditStamp {

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false, columnDefinition = "datetime")
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
