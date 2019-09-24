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
 *
 */

package com.nike.cerberus.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.signalfx.codahale.metrics.MetricBuilder;

import java.util.function.Supplier;

public class CallbackLongGauge implements Metric, Gauge<Long> {

  private final Supplier<Long> supplier;

  public CallbackLongGauge(Supplier<Long> supplier) {
    this.supplier = supplier;
  }

  @Override
  public Long getValue() {
    return supplier.get();
  }

  public static class Builder implements MetricBuilder<CallbackLongGauge> {

    private final Supplier<Long> supplier;

    public static Builder getInstance(Supplier<Long> supplier) {
      return new Builder(supplier);
    }

    private Builder(Supplier<Long> supplier) {
      this.supplier = supplier;
    }

    @Override
    public CallbackLongGauge newMetric() {
      return new CallbackLongGauge(supplier);
    }

    @Override
    public boolean isInstance(Metric metric) {
      return metric instanceof CallbackLongGauge;
    }
  }
}
