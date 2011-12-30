package org.vaadin.consolewindow.client;

import java.util.HashMap;
import java.util.Map;

import org.vaadin.consolewindow.client.message.InitMessage;
import org.vaadin.consolewindow.client.message.LayoutProblemMessage;
import org.vaadin.consolewindow.client.message.LogMessage;
import org.vaadin.consolewindow.client.message.MessageType;
import org.vaadin.consolewindow.client.message.UIDLMessage;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.SimpleTree;

public class ConsoleWindow implements RemoteMessageHandler {

    private final RootPanel root;
    private final CheckBox gwtBox = new CheckBox("Dev mode");
    private final Map<Integer, ConsoleSession> sessions = new HashMap<Integer, ConsoleSession>();

    private RemoteWindow remoteWindow;

    public ConsoleWindow() {
        root = RootPanel.get();
        FlowPanel buttonPanel = new FlowPanel();
        buttonPanel.add(new Button("Clear", new ClickHandler() {
            public void onClick(ClickEvent event) {
                for (ConsoleSession session : sessions.values()) {
                    session.removeFromParent();
                    while (session.getWidgetCount() > 0) {
                        session.remove(0);
                    }
                }
            }
        }));
        buttonPanel.add(new Button("Restart", new ClickHandler() {
            public void onClick(ClickEvent event) {
                remoteWindow.sendMessage(MessageType.RESTART, null);
            }
        }));
        buttonPanel.add(new Button("Analyze layouts", new ClickHandler() {
            public void onClick(ClickEvent event) {
                remoteWindow.sendMessage(MessageType.ANALYZE_LAYOUTS, null);
            }
        }));
        buttonPanel.add(new Button("Select component", new ClickHandler() {
            public void onClick(ClickEvent event) {
                Label label = new Label("--");
                // Find the last session
                int widgetCount = root.getWidgetCount();
                if (widgetCount <= 1) {
                    return;
                }

                ConsoleSession lastSession = (ConsoleSession) root
                        .getWidget(widgetCount - 1);
                lastSession
                        .add(new HTML(
                                "<i>Use mouse to select a component or click ESC to exit highlight mode.</i>"));
                lastSession.add(label);

                remoteWindow.sendMessage(MessageType.SELECT_START,
                        label.getElement());
            }
        }));

        gwtBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                boolean selected = event.getValue().booleanValue();
                remoteWindow.sendMessage(MessageType.DEVMODE,
                        selected ? JavaScriptObject.createObject() : null);
            }
        });
        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                // Outer dimensions not available in all browsers (returns -1 if
                // not available)
                int width = Math.max(getWindowOuterWidth(), event.getWidth());
                int height = Math.max(getWindowOuterHeight(), event.getHeight());
                String cookieValue = width + "x" + height;
                Cookies.setCookie(WindowDebugConsole.SIZE_COOKIE, cookieValue,
                        null, null, "/", false);
            }
        });
        buttonPanel.add(gwtBox);
        root.add(buttonPanel);
    }

    private native static int getWindowOuterHeight() /*-{
		return $wnd.outerHeight || -1;
    }-*/;

    private native static int getWindowOuterWidth() /*-{
		return $wnd.outerWidth || -1;
    }-*/;

    public void register() {
        RemoteWindow opener = getOpener();
        if (opener.canSend()) {
            opener.announce();
        } else {
            WindowDebugConsole
                    .logToConsole("Could not announce to the parent window");
        }
    }

    private static native RemoteWindow getOpener()/*-{
		return $wnd.opener;
    }-*/;

    public void onMessage(String typeString, JavaScriptObject data,
            RemoteWindow source) {
        MessageType type = MessageType.valueOf(typeString);
        switch (type) {
        case LOG_MESSAGE:
            LogMessage message = data.cast();
            getSession(message.getConsole()).log(message);
            break;
        case UIDL:
            UIDLMessage uidl = data.cast();
            getSession(uidl.getConsole()).logUIDL(uidl);
            break;
        case ANNOUNCE:
            announce(source);
            break;
        case INIT:
            InitMessage initMessage = data.cast();
            startNewSession(initMessage);
            break;
        case LAYOUT_PROBLEMS:
            LayoutProblemMessage layoutMessage = data.cast();
            getSession(layoutMessage.getConsole()).showLayoutProblems(
                    layoutMessage);
        default:
            WindowDebugConsole.logToConsole("Unknown message type: " + type);
            break;
        }
    }

    private void announce(RemoteWindow source) {
        remoteWindow = source;
    }

    private ConsoleSession getSession(int console) {
        ConsoleSession session = sessions.get(Integer.valueOf(console));
        if (session != null && session.getParent() == null) {
            root.add(session);
        }
        return session;
    }

    private void startNewSession(InitMessage initMessage) {
        // Collapse all previous sessions
        int itemCount = root.getWidgetCount();
        for (int i = 0; i < itemCount; i++) {
            Widget childWidget = root.getWidget(i);
            if (childWidget instanceof SimpleTree) {
                SimpleTree childNode = (SimpleTree) childWidget;
                childNode.close();
            }
        }

        String location = initMessage.getLocation();
        gwtBox.setValue(Boolean.valueOf(location
                .matches(".*\\?(.*&)?gwt\\.codesvr=.*")));

        ConsoleSession session = new ConsoleSession(initMessage, remoteWindow);
        sessions.put(Integer.valueOf(initMessage.getConsole()), session);
        root.add(session);
    }

}
