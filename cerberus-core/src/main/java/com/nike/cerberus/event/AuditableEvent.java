/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
