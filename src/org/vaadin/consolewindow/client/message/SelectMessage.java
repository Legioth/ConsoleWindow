package org.vaadin.consolewindow.client.message;

import com.google.gwt.core.client.JavaScriptObject;

public final class SelectMessage extends JavaScriptObject {

    protected SelectMessage() {
        // JSO constructor
    }

    public native String getPid() /*-{
		return this.pid;
    }-*/;

    public native boolean isOnServer() /*-{
		return this.onServer;
    }-*/;

    public native static JavaScriptObject create(String paintableId,
            boolean selectOnServer) /*-{
		return {
			pid : paintableId,
			onServer : selectOnServer
		}
    }-*/;

}
