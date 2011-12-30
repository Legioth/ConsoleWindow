package org.vaadin.consolewindow.client.message;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsDate;

public final class InitMessage extends JavaScriptObject {
    protected InitMessage() {

    }

    public native String getLocation() /*-{
		return this.location;
    }-*/;

    public native JsDate getTime() /*-{
		return this.time;
    }-*/;

    public native int getConsole() /*-{
		return this.console;
    }-*/;

    public native static InitMessage create(String href, int id) /*-{
		return {
			location : href,
			console : id,
			time : new Date()
		};
    }-*/;

}
