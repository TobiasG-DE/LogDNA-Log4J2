package eu.mizerak.alemiz;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.*;

@Plugin(name = "LogDNAAsyncAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class LogDNAScheduledAsyncAppender extends LogDNASimpleAppender {

    private final ScheduledExecutorService tickExecutor;
    private final ScheduledFuture<?> tickFuture;

    private final Queue<JsonObject> messagesQueue = new ConcurrentLinkedQueue<>();

    protected LogDNAScheduledAsyncAppender(String name, Layout<? extends Serializable> layout, String hostname, String appName, String token, boolean stackTrace, boolean supportMdc, String[] tags, Level minimalLogLevel) {
        super(name, layout, hostname, appName, token, stackTrace, supportMdc, tags, minimalLogLevel);
        this.tickExecutor = Executors.newSingleThreadScheduledExecutor();
        this.tickFuture = this.tickExecutor.scheduleAtFixedRate(this::onTick, 0, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    protected OkHttpClient initHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(15, TimeUnit.SECONDS);
        builder.readTimeout(15, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        builder.connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES));
        return builder.build();
    }

    @Override
    public void append(LogEvent logEvent) {
        Layout<? extends Serializable> layout = this.getLayout();
        String message = new String(layout.toByteArray(logEvent));

        JsonObject line = this.createLineEntry(message, logEvent);
        this.messagesQueue.offer(line);
    }

    private void onTick() {
        JsonArray lines = new JsonArray();
        JsonObject line;
        while ((line = this.messagesQueue.poll()) != null) {
            lines.add(line);
        }

        if (lines.size() < 1) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.add("lines", lines);

        Request request = this.createRequest(payload.toString());
        this.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    body.close();
                }
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.err.println("Failed to upload logs to LogDNA:");
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() {
        this.tickFuture.cancel(false);
        super.stop();
    }
}
