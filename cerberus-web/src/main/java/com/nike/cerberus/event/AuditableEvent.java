package com.nike.cerberus.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class AuditableEvent extends ApplicationEvent {

  @Getter private final AuditableEventContext auditableEventContext;

  /**
   * Create a new {@code ApplicationEvent}.
   *
   * @param source the object on which the event initially occurred or with which the event is
   *     associated (never {@code null})
   * @param auditableEventContext
   */
  public AuditableEvent(Object source, AuditableEventContext auditableEventContext) {
    super(source);
    this.auditableEventContext = auditableEventContext;
  }

  @Override
  public String toString() {
    return auditableEventContext.getEventAsString();
  }
}
