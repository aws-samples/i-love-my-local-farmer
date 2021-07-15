package com.ilmlf.delivery.api.handlers;

import org.mockito.Mockito;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

public abstract class MockProvider {

  public static MetricsLogger getMockMetricsLogger() {
    MetricsLogger metricsLogger = Mockito.mock(MetricsLogger.class);
    Mockito.when(metricsLogger.putDimensions(Mockito.any())).thenReturn(metricsLogger);
    Mockito.when(metricsLogger.putMetric(Mockito.any(), Mockito.anyDouble(), Mockito.any())).thenReturn(metricsLogger);

    return metricsLogger;
  }
}
