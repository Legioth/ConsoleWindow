package org.vaadin.consolewindow.client;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.RunAsyncCallback;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Console;
import com.vaadin.terminal.gwt.client.NullConsole;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.VDebugConsole;
import com.vaadin.terminal.gwt.client.ValueMap;

public class WindowDebugConsole extends VDebugConsole {
    public static final String SIZE_COOKIE = "VDebugWindow.size";
    private Console impl;

    @Override
    public void init() {
        if (getWindowName().startsWith("debug")) {
            WindowDebugConsole.logToConsole("Initing debug window");
            impl = new NullConsole();
            // Allow the actual console implementation to be lazily loaded
            GWT.runAsync(new RunAsyncCallback() {
                public void onFailure(Throwable reason) {
                    WindowDebugConsole
                            .logToConsole("Debug console could not be loaded");
                }

                public void onSuccess() {
                    ConsoleWindow standaloneConsole = new ConsoleWindow();
                    installMessageHandler(standaloneConsole);
                    standaloneConsole.register();
                }
            });
        } else {
            MainWindowConsole mainWindowConsole = new MainWindowConsole();
            impl = mainWindowConsole;
            installMessageHandler(mainWindowConsole);
        }
    }

    @Override
    public void log(String msg) {
        impl.log(msg);
    }

    @Override
    public void error(String msg) {
        impl.error(msg);
    }

    @Override
    public void dirUIDL(ValueMap u, ApplicationConfiguration cnf) {
        impl.dirUIDL(u, cnf);
    }

    @Override
    public void log(Throwable e) {
        impl.log(e);
    }

    @Override
    public void error(Throwable e) {
        impl.error(e);
    }

    @Override
    public void printObject(Object msg) {
        impl.printObject(msg);
    }

    @Override
    public void printLayoutProblems(ValueMap meta,
            ApplicationConnection applicationConnection,
            Set<Paintable> zeroHeightComponents,
            Set<Paintable> zeroWidthComponents) {
        impl.printLayoutProblems(meta, applicationConnection,
                zeroHeightComponents, zeroWidthComponents);
    }

    public static void logToConsole(Object... objects) {
        JsArrayMixed array = JavaScriptObject.createArray().cast();
        for (Object object : objects) {
            if (object instanceof String) {
                String string = (String) object;
                array.push(string);
            } else if (object instanceof JavaScriptObject) {
                JavaScriptObject jso = (JavaScriptObject) object;
                array.push(jso);
            }
        }
        logToConsole(array);
    }

    private static native void logToConsole(JsArrayMixed message)/*-{
		if ($wnd.console) {
			if (typeof $wnd.console.log === 'function') {
				$wnd.console.log.apply($wnd.console, message);
			} else {
				$wnd.console.log(message);
			}
		}
    }-*/;

    private native void installMessageHandler(RemoteMessageHandler handler)/*-{
		$wnd.onMessage = function(message, data, source) {
			//@org.vaadin.consolewindow.client.WindowDebugConsole::logToConsole(Lcom/google/gwt/core/client/JsArrayMixed;)(["Got " + message +":", data, "from", source]);
			handler.@org.vaadin.consolewindow.client.RemoteMessageHandler::onMessage(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Lorg/vaadin/consolewindow/client/RemoteWindow;)(message, data, source);
		}
    }-*/;

    public static native String getWindowName() /*-{
		return $wnd.name;
    }-*/;

}
