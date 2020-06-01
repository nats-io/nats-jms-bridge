
# NATS Bridge Beta2


Added config changes to demonstrate using different IBM MQ servers and passing NATs client config via the admin YAML config.

* #127 Log messages using trace configurable via logback.xml
* #126 Log bridge mapping at startup
* #125 Added NATS connection metrics.  
* #129 Interface proposal for Transforms
* #124 Make JSSL / SSL configurable.
* #128 Uptime in the health check


## Logback config

You can set up `startup` log to see parmas at start up.
You can set up `runtime` log to see low level details about sending messages at runtime.


#### Example logback.xml which can be placed in project/config
```
<configuration>

    <conversionRule conversionWord="clr" converterClass="org.springframework.boot.logging.logback.ColorConverter" />
    <conversionRule conversionWord="wex" converterClass="org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter" />
    <conversionRule conversionWord="wEx" converterClass="org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter" />
    <property name="CONSOLE_LOG_PATTERN" value="${CONSOLE_LOG_PATTERN:-%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"/>

    <property name="LOGS" value="./logs" />

    <appender name="INFOLEVEL" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOGS}/InfoFile.log</file>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern>%d %p %C{1.} [%t] %m%n</Pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOGS}/archived/InfoFile-%d{yyyy-MM-dd}.%i.log
            </fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <appender name="TRACELEVEL" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOGS}/TraceFile.log</file>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern>%d %p %C{1.} [%t] %m%n</Pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOGS}/archived/TraceFile-%d{yyyy-MM-dd}.%i.log
            </fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!--root level="debug"-->
    <root level="info">
        <appender-ref ref="STDOUT" />
    </root>

    <logger name="startup" level="info">
        <appender-ref ref="INFOLEVEL" />
    </logger>

    <logger name="runtime" level="trace">
        <appender-ref ref="TRACELEVEL" />
    </logger>

</configuration>

```

## Uptime is in health check

The uptime in seconds is now in the health check.

```bash

$ curl http://localhost:8080/manage/health | jq .
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   254    0   254    0     0  28222      0 --:--:-- --:--:-- --:--:-- 28222
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 2000796545024,
        "free": 1643018698752,
        "threshold": 10485760
      }
    },
    "healthChecker": {
      "status": "UP",
      "details": {
        "NATS_MessageBridge": "Available",
        "upTimeSeconds": 314
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}

```

## Setting up JSSL

To set up JSSL, pass the following properties to the NATS cluster config.

```java


    public static final String PFX_TLS = "io.nats.client.tls.";
    public static final String JSSL_ENABLE =   PFX_TLS + "jssl.enable";
    public static final String JSSL_TRUST_STORE_PATH =   PFX_TLS + "truststore.path";
    public static final String JSSL_KEY_STORE_PATH =   PFX_TLS + "keystore.path";
    public static final String JSSL_ALGORITHM =   PFX_TLS + "algorithm";
    public static final String JSSL_STORE_PWD =   PFX_TLS + "store.password";
    public static final String JSSL_TRUST_PWD =   PFX_TLS + "trust.password";
```

This can be done in the NATS cluster config in the YAML file for the MQ NATS Bridge admin.

 
