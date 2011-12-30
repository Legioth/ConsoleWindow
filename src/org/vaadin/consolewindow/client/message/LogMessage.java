package org.vaadin.consolewindow.client.message;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsDate;

public final class LogMessage extends JavaScriptObject {
    protected LogMessage() {

    }

    public native String getMessage()/*-{
		return this.message;
    }-*/;

    public native boolean isError() /*-{
		return this.error;
    }-*/;

    public native JsDate getTime()/*-{
		return this.time;
    }-*/;

    public native String getLocation() /*-{
		return this.location;
    }-*/;

    public native int getConsole() /*-{
		return this.console;
    }-*/;

    public static native LogMessage make(String m, boolean isError,
            String l, int consoleId) /*-{
		return {
			message : m,
			location : l,
			console : consoleId,
			error : isError,
			time : new Date()
		};
    }-*/;

}