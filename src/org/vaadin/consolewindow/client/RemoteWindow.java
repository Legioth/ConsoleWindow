package org.vaadin.consolewindow.client;

import org.vaadin.consolewindow.client.message.MessageType;

import com.google.gwt.core.client.JavaScriptObject;

public final class RemoteWindow extends JavaScriptObject {
    protected RemoteWindow() {
        // JSO constructor
    }

    public String sendMessage(MessageType type, JavaScriptObject data) {
        return sendMessage(type.name(), data);
    }

    private native String sendMessage(String type, JavaScriptObject data)/*-{
		return this.onMessage(type, data, $wnd);
    }-*/;

    public boolean canSend() {
        return isOpen() && isMessageHandlerPresent();
    }

    public native boolean isMessageHandlerPresent()/*-{
		// Can't use typeof or instanceof because of cross-window issues
		return !!(this.onMessage && this.onMessage.call);
    }-*/;

    public native boolean isOpen()/*-{
		try {
			return !this.closed;
		} catch (err) {
			$wnd.console.log(err);
			return false;
		}
    }-*/;

    public native boolean isLoaded()/*-{
		return this.document.readystate == 'complete';
    }-*/;

    public void announce() {
        sendMessage(MessageType.ANNOUNCE, null);
    }
}
