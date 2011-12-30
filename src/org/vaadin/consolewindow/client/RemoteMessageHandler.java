package org.vaadin.consolewindow.client;

import com.google.gwt.core.client.JavaScriptObject;

public interface RemoteMessageHandler {
    public void onMessage(String type, JavaScriptObject data, RemoteWindow source);
}
