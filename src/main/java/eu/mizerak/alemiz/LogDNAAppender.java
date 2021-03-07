package eu.mizerak.alemiz;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.okhttp.*;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Plugin(name = "LogDNAAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class LogDNAAppender extends AbstractAppender {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected final String hostname;
    protected final String appName;
    protected final String token;
    protected final boolean stackTrace;
    protected final boolean supportMdc;

    protected final OkHttpClient client = new OkHttpClient();
    protected String httpUrl = "https://logs.logdna.com/logs/ingest";

    protected LogDNAAppender(String name, Layout<? extends Serializable> layout, String hostname, String appName, String token, boolean stackTrace, boolean supportMdc) {
        super(name, null, layout, false, null);
        this.hostname = hostname;
        this.appName = appName;
        this.token = token;
        this.stackTrace = stackTrace;
        this.supportMdc = supportMdc;
        this.client.setConnectTimeout(15, TimeUnit.SECONDS);
        this.client.setReadTimeout(15, TimeUnit.SECONDS);
    }

    @Override
    public void append(LogEvent logEvent) {
        Layout<? extends Serializable> layout = this.getLayout();
        String message = new String(layout.toByteArray(logEvent));

        JsonArray lines = new JsonArray();
        lines.add(this.createLineEntry(message, logEvent));

        JsonObject payload = new JsonObject();
        payload.add("lines", lines);
        this.sendHtmlRequest(payload.toString());
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
            for (Map.Entry<String, String> entry: logEvent.getContextData().toMap().entrySet()) {
                meta.addProperty(entry.getKey(), entry.getValue());
            }
        }
        line.add("meta", meta);
        return line;
    }

    protected void sendHtmlRequest(String payload) {
        RequestBody body = RequestBody.create(JSON, payload);
        Request.Builder builder = new Request.Builder();
        builder.url(this.httpUrl+"?hostname="+encode(this.hostname)+"&now="+encode(String.valueOf(System.currentTimeMillis())));
        builder.addHeader("apikey", this.token);
        builder.addHeader("User-Agent", "LogDna Log4j2 Appender");
        builder.addHeader("Accept", "application/json");
        builder.post(body);

        Request request = builder.build();
        try {
            Response response = this.client.newCall(request).execute();
            response.body().close();
        } catch (IOException e) {
            System.err.println("Failed to upload logs to LogDNA!");
            e.printStackTrace();
        }
    }

    private String encode(String str){
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }
}
