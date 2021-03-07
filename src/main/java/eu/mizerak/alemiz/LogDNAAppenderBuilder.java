package eu.mizerak.alemiz;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Builder;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LogDNAAppenderBuilder implements Builder<LogDNAAppender> {

    @PluginBuilderAttribute
    private String name = "LogDNAAppender";

    @PluginElement("Layout")
    private Layout<? extends Serializable> layout;

    @PluginBuilderAttribute
    @Required
    private String hostname;

    @PluginBuilderAttribute
    @Required
    private String appName;

    @PluginBuilderAttribute(sensitive = true)
    @Required
    private String token;

    @PluginBuilderAttribute
    @Required
    private boolean stackTrace;

    @PluginBuilderAttribute
    @Required
    private boolean supportMdc;

    @PluginBuilderAttribute
    private boolean async = false;

    public LogDNAAppenderBuilder() {
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.hostname = "localhost";
        }
    }

    public LogDNAAppenderBuilder name(String name) {
        this.name = name;
        return this;
    }

    public LogDNAAppenderBuilder layout(Layout<? extends Serializable> layout) {
        this.layout = layout;
        return this;
    }

    public LogDNAAppenderBuilder hostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public LogDNAAppenderBuilder appName(String appName) {
        this.appName = appName;
        return this;
    }

    public LogDNAAppenderBuilder token(String token) {
        this.token = token;
        return this;
    }

    public LogDNAAppenderBuilder logStackTrace(boolean stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    public LogDNAAppenderBuilder supportMdc(boolean supportMdc) {
        this.supportMdc = supportMdc;
        return this;
    }

    public LogDNAAppenderBuilder async(boolean async) {
        this.async = async;
        return this;
    }

    @Override
    public LogDNAAppender build() {
        if (this.async) {
            return new LogDNAAsyncAppender(this.name, this.layout, this.hostname, this.appName, this.token, this.stackTrace, this.supportMdc);
        }
        return new LogDNAAppender(this.name, this.layout, this.hostname, this.appName, this.token, this.stackTrace, this.supportMdc);
    }
}
