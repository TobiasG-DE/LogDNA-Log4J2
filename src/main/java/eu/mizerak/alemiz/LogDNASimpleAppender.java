package eu.mizerak.alemiz;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Plugin(name = "LogDNAAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class LogDNASimpleAppender extends AbstractAppender {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected final String hostname;
    protected final String appName;
    protected final String token;
    protected final String tags;
    protected final boolean stackTrace;
    protected final boolean supportMdc;
    protected final Level minimalLogLevel;

    protected final OkHttpClient client;

    protected LogDNASimpleAppender(String name, Layout<? extends Serializable> layout, String hostname, String appName, String token, boolean stackTrace, boolean supportMdc, String[] tags, Level level) {
        super(name, null, layout, false, null);
        this.hostname = hostname;
        this.appName = appName;
        this.token = token;
        this.stackTrace = stackTrace;
        this.supportMdc = supportMdc;
        this.tags = tags == null ? null : String.join(",", tags);
        this.minimalLogLevel = level;
        this.client = this.initHttpClient();
    }

    protected OkHttpClient initHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(15, TimeUnit.SECONDS);
        builder.readTimeout(15, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        return builder.build();
    }

    @Override
    public void append(LogEvent logEvent) {
        if(logEvent.getLevel().intLevel() <= this.minimalLogLevel.intLevel()) return;

        Layout<? extends Serializable> layout = this.getLayout();
        String message = new String(layout.toByteArray(logEvent));

        JsonArray lines = new JsonArray();
        lines.add(this.createLineEntry(message, logEvent));

        JsonObject payload = new JsonObject();
        payload.add("lines", lines);

        Request request = this.createRequest(payload.toString());
        try {
            Response response = this.client.newCall(request).execute();
            response.body().close();
        } catch (IOException e) {
            System.err.println("Failed to upload logs to LogDNA!");
            e.printStackTrace();
        }
    }

    protected JsonObject createLineEntry(String message, LogEvent logEvent) {
        StringBuilder builder = new StringBuilder(message);
        if (logEvent.getThrownProxy() != null && this.stackTrace) {
            ThrowableProxy throwable = logEvent.getThrownProxy();
            builder.append("\n\n").append(throwable.getClass()).append(": ").append(throwable.getMessage());

            ThrowableProxy cause = throwable.getCauseProxy();
            while (cause != null) {
                builder.append("\n\n").append(cause.getCauseStackTraceAsString(""));
                cause = cause.getCauseProxy();
            }
        }

        JsonObject line = new JsonObject();
        line.addProperty("timestamp", logEvent.getInstant().getEpochMillisecond());
        line.addProperty("level", logEvent.getLevel().toString());
        line.addProperty("app", this.appName);
        line.addProperty("line", builder.toString());

        JsonObject meta = new JsonObject();
        meta.addProperty("logger", logEvent.getLoggerName());
        if (this.supportMdc && !logEvent.getContextData().isEmpty()) {
            for (Map.Entry<String, String> entry : logEvent.getContextData().toMap().entrySet()) {
                meta.addProperty(entry.getKey(), entry.getValue());
            }
        }
        line.add("meta", meta);
        return line;
    }

    protected Request createRequest(String payload) {
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
        urlBuilder.scheme("https");
        urlBuilder.host("logs.logdna.com");
        urlBuilder.addPathSegment("logs");
        urlBuilder.addPathSegment("ingest");
        urlBuilder.addQueryParameter("hostname", this.hostname);
        if (this.tags != null) {
            urlBuilder.addQueryParameter("tags", this.tags);
        }
        urlBuilder.addQueryParameter("now", String.valueOf(System.currentTimeMillis()));

        Request.Builder builder = new Request.Builder();
        builder.url(urlBuilder.build());
        builder.addHeader("apikey", this.token);
        builder.addHeader("User-Agent", "LogDna Log4j2 Appender");
        builder.addHeader("Accept", "application/json");
        builder.post(RequestBody.create(JSON, payload));
        return builder.build();
    }
}
