package eu.mizerak.alemiz;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.*;

@Plugin(name = "LogDNAAsyncAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class LogDNAAsyncAppender extends LogDNAAppender {

    private final ScheduledExecutorService tickExecutor;
    private final ScheduledFuture<?> tickFuture;

    private final Queue<JsonObject> messagesQueue = new ConcurrentLinkedQueue<>();

    protected LogDNAAsyncAppender(String name, Layout<? extends Serializable> layout, String hostname, String appName, String token, boolean stackTrace, boolean supportMdc) {
        super(name, layout, hostname, appName, token, stackTrace, supportMdc);
        this.tickExecutor = Executors.newSingleThreadScheduledExecutor();
        this.tickFuture = this.tickExecutor.scheduleAtFixedRate(this::onTick, 0, 200, TimeUnit.MILLISECONDS);
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
        this.sendHtmlRequest(payload.toString());
    }

    @Override
    public void stop() {
        this.tickFuture.cancel(false);
        super.stop();
    }
}
