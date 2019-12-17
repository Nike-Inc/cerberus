/*
 * Copyright (c) 2019 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.nike.cerberus.event.Event;
import com.nike.cerberus.event.processor.EventProcessor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service that processes events throughout the Cerberus Management Service asynchronously using
 * Hystrix. Multiple processors can be registered with this service and any time an event is
 * ingested this will send the event asynchronously to all processors.
 *
 * <p>The idea behind this is you can log the events to stdout / log files and send data to data
 * stores and or monitoring services
 */
@Component
public class EventProcessorService {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  private static final String COMMAND_GROUP = "events";
  private static final String THREAD_POOL_NAME = "event-processor-tp";
  private static final String INGEST_COMMAND = "process-event-command";

  private final List<EventProcessor> eventProcessors;

  @Autowired
  public EventProcessorService(List<EventProcessor> eventProcessors) {
    this.eventProcessors = eventProcessors;
  }

  /**
   * Asynchronously ingests an event to process with all registered event processors.
   *
   * @param event The Cerberus event, ex: a principal deleting a SDB.
   */
  public void ingestEvent(Event event) {
    try {
      new HystrixCommand<Void>(
          HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(COMMAND_GROUP))
              .andCommandKey(HystrixCommandKey.Factory.asKey(INGEST_COMMAND))
              .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(THREAD_POOL_NAME))) {

        @Override
        protected Void run() throws Exception {
          eventProcessors.forEach(processor -> processEvent(event, processor));
          return null;
        }
      }.queue();
    } catch (Throwable t) {
      log.error("There was an issue ingesting event", t);
      if (t.getCause() instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Asynchronously processes an event with a single processor.
   *
   * @param event The Cerberus event, ex: a principal deleting a SDB.
   * @param processor The event processor
   */
  private void processEvent(Event event, EventProcessor processor) {
    try {
      new HystrixCommand<Void>(
          HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(COMMAND_GROUP))
              .andCommandKey(HystrixCommandKey.Factory.asKey(processor.getName()))
              .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(THREAD_POOL_NAME))) {

        @Override
        protected Void run() throws Exception {
          processor.process(event);
          return null;
        }
      }.queue();
    } catch (Throwable t) {
      log.error("There was an issue processing event", t);
      if (t.getCause() instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
