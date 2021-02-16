# Simple LogDNA log publisher
This library provides an appender for [Log4J2](https://logging.apache.org/log4j/2.x/) which allows log publishing to [LogDNA](https://logdna.com/).  
This backend use LogDNA [ingestion api](https://docs.logdna.com/v1.0/reference#api).

## Usage example
### During runtime (using code):
```java
        LogDNAAppenderBuilder builder = new LogDNAAppenderBuilder();
        builder.token(token); // Ingest token
        builder.appName(appName); // Application name shown in LogDNA reports
        builder.logStackTrace(true); // Handle stacktraces from logger
        builder.supportMdc(true); // Send Log4j2's Context Data as metadata which are searchable
        builder.layout(patternLayout); // Your log pattern here
        builder.async(true); // Whatever send logs asynchronously without blocking current thread
        builder.hostname(hostName); // If not set System hostname will be used 

        // Create and start appender
        LogDNAAppender appender = builder.build();
        appender.start();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        // Add appender to root logger and config
        config.addAppender(appender);
        context.getRootLogger().addAppender(config.getAppender(appender.getName()));
        context.updateLoggers();
```
### Using configuration file:
Add to appenders list:
```xml
        <LogDNAAppender name="LogDNA">
            <token>your-token-here</token>
            <appName>app-name</appName>
            <logStackTrace>true</logStackTrace>
            <supportMdc>true</supportMdc>
            <async>true</async>
            <PatternLayout pattern="%date %level method: %class{1}.%method (%file:%line) - %message%n"/>
        </LogDNAAppender>
```
Dont forget to mention appender inside of loggers list:
```xml
    <Loggers>
        <Root level="all">
            <!--Your loggers here-->
            <AppenderRef ref="LogDNA" level="debug"/>
        </Root>
    </Loggers>
```

## Libraries
- Using [OkHttp](https://github.com/square/okhttp) client for http calls.
- Google [Gson](https://github.com/google/gson) for work with json objects.
- And of course Log4J2, version 2.11.2 