package com.ilmlf.delivery.api;

import java.util.List;
import java.util.Map;
import lombok.Data;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.AlarmProps;
import software.amazon.awscdk.services.cloudwatch.AlarmWidget;
import software.amazon.awscdk.services.cloudwatch.AlarmWidgetProps;
import software.amazon.awscdk.services.cloudwatch.Dashboard;
import software.amazon.awscdk.services.cloudwatch.DashboardProps;
import software.amazon.awscdk.services.cloudwatch.GraphWidget;
import software.amazon.awscdk.services.cloudwatch.GraphWidgetProps;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricProps;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.sns.Topic;

/***
 * Class representing a dashboard for a Lambda function.
 * Graphs the invocations and errors of the function, while also creating and displaying an alarm threshold for the
 * number of errors a function can experience.
 */
public class FunctionDashboard extends Dashboard {
  private static String LAMBDA_NAMESPACE = "AWS/Lambda";
  private static String EMF_NAMESPACE = "aws-embedded-metrics";
  private static String DIMENSION_FUNCTION_NAME = "FunctionName";
  private static String DIMENSION_SERVICE_NAME = "ServiceName";
  private static String DIMENSION_LOG_GROUP = "LogGroup";
  private static String DIMENSION_SERVICE_TYPE = "ServiceType";
  private static String SERVICE_TYPE_LAMBDA = "AWS::Lambda::Function";
  private static int FULL_DASHBOARD_WIDTH = 24;
  private static int HALF_DASHBOARD_WIDTH = FULL_DASHBOARD_WIDTH / 2;
  private static int QUARTER_DASHBOARD_WIDTH = FULL_DASHBOARD_WIDTH / 4;
  private static int STANDARD_WIDGET_HEIGHT = 6;

  public FunctionDashboard(Construct scope, String id, FunctionDashboardProps props) {
    super(scope, id, DashboardProps.builder()
        .dashboardName(props.dashboardName)
        .build());

    createGetSlotsWidgetsAndAlarms(props);
    createCreateSlotsWidgetsAndAlarms(props);
    createBookDeliveryWidgetsAndAlarms(props);
  }

  private void createGetSlotsWidgetsAndAlarms(FunctionDashboardProps props) {
    // The "FunctionName" dimension is a custom dimension added by the Lambdas to differentiate between functions, and
    // corresponds to the props._ApiMethodName.
    // It is not the same as props._FunctionName, which is a token that identifies the function within Cloudformation
    Map<String, String> dimensions = Map.of(
        DIMENSION_FUNCTION_NAME, props.getSlotsApiMethodName,
        DIMENSION_SERVICE_NAME, props.getSlotsFunctionName,
        DIMENSION_LOG_GROUP, props.getSlotsFunctionName,
        DIMENSION_SERVICE_TYPE, SERVICE_TYPE_LAMBDA
    );

    createCommonWidgetsAndAlarms(props.getSlotsFunctionName, props.getSlotsApiMethodName, props.alarmTopic);

    Metric noSlotsFoundMetric = new Metric(MetricProps.builder()
        .metricName("NoSlotsFound")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("GetSlots When Farm Has No Slots")
        .build());

    Metric slotsReturnedMetric = new Metric(MetricProps.builder()
        .metricName("SlotsReturned")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("GetSlots When Farm Has No Slots")
        .build());

    Metric sqlExceptionMetric = new Metric(MetricProps.builder()
        .metricName("SqlException")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("GetSlots SqlException")
        .build());

    Alarm sqlExceptionAlarm = new Alarm(this, props.getSlotsApiMethodName + "SqlExceptionAlarm", AlarmProps.builder()
        .alarmName(props.getSlotsFunctionName + "-SqlExceptionAlarm")
        .metric(sqlExceptionMetric)
        .evaluationPeriods(1)
        .threshold(1)
        .build());

    sqlExceptionAlarm.addAlarmAction(new SnsAction(props.alarmTopic));

    GraphWidget noSlotsFoundWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .left(List.of(noSlotsFoundMetric))
        .title(props.getSlotsApiMethodName + "-NoSlotsFound")
        .build());

    GraphWidget slotsReturnedWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .left(List.of(slotsReturnedMetric))
        .title(props.getSlotsApiMethodName + "-SlotsReturned")
        .build());

    AlarmWidget sqlExceptionWidget = new AlarmWidget(AlarmWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(FULL_DASHBOARD_WIDTH)
        .alarm(sqlExceptionAlarm)
        .title(props.getSlotsApiMethodName + "-SqlExceptions")
        .build());

    this.addWidgets(noSlotsFoundWidget, slotsReturnedWidget, sqlExceptionWidget);
  }

  private void createCreateSlotsWidgetsAndAlarms(FunctionDashboardProps props) {
    Map<String, String> dimensions = Map.of(
        DIMENSION_FUNCTION_NAME, props.createSlotsApiMethodName,
        DIMENSION_SERVICE_NAME, props.createSlotsFunctionName,
        DIMENSION_LOG_GROUP, props.createSlotsFunctionName,
        DIMENSION_SERVICE_TYPE, SERVICE_TYPE_LAMBDA
    );

    createCommonWidgetsAndAlarms(props.createSlotsFunctionName, props.createSlotsApiMethodName, props.alarmTopic);

    Metric invalidSlotListMetric = new Metric(MetricProps.builder()
        .metricName("InvalidSlotList")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("CreateSlots Received Invalid Slot List")
        .build());

    Metric failedToSaveSlotsMetric = new Metric(MetricProps.builder()
        .metricName("FailedToSaveSlots")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("CreateSlots Failed to Save Slots")
        .build());

    Metric exceptionMetric = new Metric(MetricProps.builder()
        .metricName("CreateSlotsException")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("CreateSlots Insert Exception")
        .build());

    Alarm failedToSaveSlotsAlarm = new Alarm(this, props.createSlotsApiMethodName + "FailedToSaveSlotsAlarm",
        AlarmProps.builder()
          .alarmName(props.createSlotsFunctionName + "-FailedToSaveSlotsAlarm")
          .metric(failedToSaveSlotsMetric)
          .evaluationPeriods(1)
          .threshold(1)
          .build());

    Alarm exceptionAlarm = new Alarm(this, props.createSlotsApiMethodName + "ExceptionAlarm", AlarmProps.builder()
        .alarmName(props.createSlotsFunctionName + "-ExceptionsAlarm")
        .metric(exceptionMetric)
        .evaluationPeriods(1)
        .threshold(1)
        .build());

    failedToSaveSlotsAlarm.addAlarmAction(new SnsAction(props.alarmTopic));
    exceptionAlarm.addAlarmAction(new SnsAction(props.alarmTopic));

    GraphWidget invalidSlotListWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .left(List.of(invalidSlotListMetric))
        .title(props.createSlotsApiMethodName + "-InvalidSlotList")
        .build());

    AlarmWidget failedToSaveSlotsWidget = new AlarmWidget(AlarmWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .alarm(failedToSaveSlotsAlarm)
        .title(props.createSlotsApiMethodName + "-FailedToSaveSlots")
        .build());

    AlarmWidget exceptionWidget = new AlarmWidget(AlarmWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .alarm(exceptionAlarm)
        .title(props.createSlotsApiMethodName + "-Exceptions")
        .build());

    this.addWidgets(failedToSaveSlotsWidget, exceptionWidget, invalidSlotListWidget);
  }

  private void createBookDeliveryWidgetsAndAlarms(FunctionDashboardProps props) {
    Map<String, String> dimensions = Map.of(
        DIMENSION_FUNCTION_NAME, props.bookDeliveryApiMethodName,
        DIMENSION_SERVICE_NAME, props.bookDeliveryFunctionName,
        DIMENSION_LOG_GROUP, props.bookDeliveryFunctionName,
        DIMENSION_SERVICE_TYPE, SERVICE_TYPE_LAMBDA
    );

    createCommonWidgetsAndAlarms(props.bookDeliveryFunctionName, props.bookDeliveryApiMethodName, props.alarmTopic);

    Metric invalidUserIdMetric = new Metric(MetricProps.builder()
        .metricName("InvalidUserId")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("BookDelivery Received Invalid User Id")
        .build());

    Metric deliveryBookedMetric = new Metric(MetricProps.builder()
        .metricName("DeliveryBooked")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("DeliveryBooked")
        .build());

    Metric farmAndSlotInvalidMetric = new Metric(MetricProps.builder()
        .metricName("FarmAndSlotInvalid")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("BookDelivery Farm and Slot Invalid")
        .build());

    Metric sqlExceptionMetric = new Metric(MetricProps.builder()
        .metricName("SqlException")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("BookDelivery Sql Exception")
        .build());

    Metric noAvailableDeliveryMetric = new Metric(MetricProps.builder()
        .metricName("NoAvailableDelivery")
        .namespace(EMF_NAMESPACE)
        .dimensionsMap(dimensions)
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("BookDelivery No Available Delivery")
        .build());

    Alarm sqlExceptionAlarm = new Alarm(this, props.bookDeliveryApiMethodName + "SqlExceptionAlarm", AlarmProps.builder()
        .alarmName(props.bookDeliveryFunctionName + "-SqlExceptionAlarm")
        .metric(sqlExceptionMetric)
        .evaluationPeriods(1)
        .threshold(1)
        .build());

    sqlExceptionAlarm.addAlarmAction(new SnsAction(props.alarmTopic));

    GraphWidget invalidUserIdWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(QUARTER_DASHBOARD_WIDTH)
        .left(List.of(invalidUserIdMetric))
        .title(props.bookDeliveryApiMethodName + "-InvalidUserId")
        .build());

    GraphWidget deliveryBookedWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(QUARTER_DASHBOARD_WIDTH)
        .left(List.of(deliveryBookedMetric))
        .title(props.bookDeliveryApiMethodName + "-DeliveryBooked")
        .build());

    GraphWidget farmAndSlotInvalidWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(QUARTER_DASHBOARD_WIDTH)
        .left(List.of(farmAndSlotInvalidMetric))
        .title(props.bookDeliveryApiMethodName + "-FarmAndSlotInvalid")
        .build());

    AlarmWidget sqlExceptionWidget = new AlarmWidget(AlarmWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(FULL_DASHBOARD_WIDTH)
        .alarm(sqlExceptionAlarm)
        .title(props.bookDeliveryApiMethodName + "-SqlExceptions")
        .build());

    GraphWidget noAvailableDeliveryWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(QUARTER_DASHBOARD_WIDTH)
        .left(List.of(noAvailableDeliveryMetric))
        .title(props.bookDeliveryApiMethodName + "-NoAvailableDelivery")
        .build());

    this.addWidgets(invalidUserIdWidget, deliveryBookedWidget, farmAndSlotInvalidWidget, noAvailableDeliveryWidget,
        sqlExceptionWidget);
  }

  private void createCommonWidgetsAndAlarms(String functionName, String apiMethodName, Topic alarmTopic) {
    Metric invocationMetric = new Metric(MetricProps.builder()
        .metricName("Invocations")
        .namespace(LAMBDA_NAMESPACE)
        .dimensionsMap(Map.of("FunctionName", functionName))
        .period(Duration.minutes(1))
        .statistic("SUM")
        .label("Number of Invocations")
        .build());

    Metric errorsMetric = new Metric(MetricProps.builder()
        .metricName("Errors")
        .namespace(LAMBDA_NAMESPACE)
        .dimensionsMap(Map.of("FunctionName", functionName))
        .period(Duration.minutes(5))
        .statistic("SUM")
        .label("Number of Errors")
        .build());

    Alarm errorsAlarm = new Alarm(this, apiMethodName + "ErrorsAlarm", AlarmProps.builder()
        .alarmName(functionName + "-ErrorsAlarm")
        .metric(errorsMetric)
        .evaluationPeriods(1)
        .threshold(2)
        .build());

    errorsAlarm.addAlarmAction(new SnsAction(alarmTopic));

    GraphWidget invocationWidget = new GraphWidget(GraphWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .left(List.of(invocationMetric))
        .title(apiMethodName + "-Invocations")
        .build());

    AlarmWidget errorsWidget = new AlarmWidget(AlarmWidgetProps.builder()
        .height(STANDARD_WIDGET_HEIGHT)
        .width(HALF_DASHBOARD_WIDTH)
        .alarm(errorsAlarm)
        .title(apiMethodName + "-Errors")
        .build());

    this.addWidgets(invocationWidget, errorsWidget);
  }

  @Data
  @lombok.Builder
  public static class FunctionDashboardProps {
    private String dashboardName;
    private String getSlotsFunctionName;
    private String getSlotsApiMethodName;
    private String createSlotsFunctionName;
    private String createSlotsApiMethodName;
    private String bookDeliveryFunctionName;
    private String bookDeliveryApiMethodName;
    private Topic alarmTopic;
  }
}
