package org.vaadin.consolewindow.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.consolewindow.client.message.InitMessage;
import org.vaadin.consolewindow.client.message.LayoutProblemMessage;
import org.vaadin.consolewindow.client.message.LogMessage;
import org.vaadin.consolewindow.client.message.MessageType;
import org.vaadin.consolewindow.client.message.SelectMessage;
import org.vaadin.consolewindow.client.message.UIDLMessage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.BrowserInfo;
import com.vaadin.terminal.gwt.client.Console;
import com.vaadin.terminal.gwt.client.PackageDelegate;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.ui.VWindow;

public class MainWindowConsole implements Console, RemoteMessageHandler {

    private final class HighlightModeHandler implements NativePreviewHandler {
        private final Element label;

        private HighlightModeHandler(Element label) {
            this.label = label;
        }

        public void onPreviewNativeEvent(NativePreviewEvent event) {
            if (event.getTypeInt() == Event.ONKEYDOWN
                    && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                highlightModeRegistration.removeHandler();
                deHiglight();
                return;
            }
            if (event.getTypeInt() == Event.ONMOUSEMOVE) {
                deHiglight();
                Element eventTarget = Util.getElementFromPoint(event
                        .getNativeEvent().getClientX(), event.getNativeEvent()
                        .getClientY());

                for (ApplicationConnection a : ApplicationConfiguration
                        .getRunningApplications()) {
                    Paintable paintable = Util.getPaintableForElement(a,
                            a.getView(), eventTarget);
                    if (paintable == null) {
                        paintable = Util.getPaintableForElement(a,
                                RootPanel.get(), eventTarget);
                    }
                    if (paintable != null) {
                        String pid = a.getPid(paintable);
                        highlight(paintable);
                        label.setInnerText("Currently focused  :"
                                + paintable.getClass() + " ID:" + pid);
                        event.cancel();
                        event.consume();
                        event.getNativeEvent().stopPropagation();
                        return;
                    }
                }
            }
            if (event.getTypeInt() == Event.ONCLICK) {
                deHiglight();
                event.cancel();
                event.consume();
                event.getNativeEvent().stopPropagation();
                highlightModeRegistration.removeHandler();
                Element eventTarget = Util.getElementFromPoint(event
                        .getNativeEvent().getClientX(), event.getNativeEvent()
                        .getClientY());
                for (ApplicationConnection a : ApplicationConfiguration
                        .getRunningApplications()) {
                    Paintable paintable = Util.getPaintableForElement(a,
                            a.getView(), eventTarget);
                    if (paintable == null) {
                        paintable = Util.getPaintableForElement(a,
                                RootPanel.get(), eventTarget);
                    }

                    if (paintable != null) {
                        PackageDelegate.highlightOnServer(a, paintable);
                        return;
                    }
                }
            }
            event.cancel();
        }
    }

    private HandlerRegistration highlightModeRegistration;

    private int consoleId = Random.nextInt();

    private final InitMessage initMessage = InitMessage.create(
            Window.Location.getHref(), consoleId);

    private static final class PendingMessage {
        private final MessageType type;
        private final JavaScriptObject data;

        public PendingMessage(MessageType type, JavaScriptObject data) {
            this.type = type;
            this.data = data;
        }
    }

    private RemoteWindow remoteWindow;

    private final List<PendingMessage> pendingMessages = new ArrayList<PendingMessage>();
    private boolean windowClosing = false;

    public MainWindowConsole() {
        Window.addWindowClosingHandler(new ClosingHandler() {
            public void onWindowClosing(ClosingEvent event) {
                windowClosing = true;
            }
        });

        openRemoteWindow();
        if (remoteWindow != null && remoteWindow.canSend()) {
            sendInit();
        }
    }

    private void sendInit() {
        send(MessageType.ANNOUNCE, null);
        send(MessageType.INIT, initMessage);
    }

    private void openRemoteWindow() {
        if (windowClosing) {
            // Don't open a new window if the current session is going down
            return;
        }
        String windowName = ensureWindowName();
        String debugWindowName = "debug" + windowName;
        String windowUrl = GWT.getModuleBaseURL() + "debug.html?debug";
        String codesvr = Window.Location.getParameter("gwt.codesvr");
        if (codesvr != null
                && Window.Location.getParameter("debug.devmode") != null) {
            windowUrl += "&gwt.codesvr=" + codesvr;
        }

        int width = 500;
        int height = 500;

        String sizeCookie = Cookies.getCookie(WindowDebugConsole.SIZE_COOKIE);
        if (sizeCookie != null) {
            String[] parts = sizeCookie.split("x");
            if (parts.length == 2) {
                try {
                    int cookieWidth = Integer.parseInt(parts[0]);
                    int cookieHeight = Integer.parseInt(parts[1]);
                    width = (int) Math.min(getScreenWidth() * 0.9f,
                            Math.max(100, cookieWidth));
                    height = (int) Math.min(getScreenHeight() * 0.9f,
                            Math.max(100, cookieHeight));
                } catch (NumberFormatException e) {
                    // Ignore exception
                }
            }
        }

        String startUrl = null;
        if (BrowserInfo.get().isIE()) {
            startUrl = windowUrl;
        }

        remoteWindow = getOrOpenWindow(debugWindowName, startUrl, windowUrl,
                width, height);
        if (remoteWindow == null) {
            GWT.log("Could not open popup");
            WindowDebugConsole.logToConsole("Could not open popup");
        }
    }

    private static native RemoteWindow getOrOpenWindow(String name,
            String startUrl, String url, int width, int height) /*-{
		var params = 'height='
				+ height
				+ ',width='
				+ width
				+ ',location=0,menubar=0,status=0,toolbar=0,scrollbars=1,resizable=1';
		var w = $wnd.open('', name, params);
		if (w == null) {
			//Probably a popup blocker
			return null;
		}

		if (w.location != url) {
			w.location = url;
			w.onMessage = null;
		}
		w.focus();
		return w;
    }-*/;

    private native static int getScreenWidth()/*-{
		return $wnd.screen.width;
    }-*/;

    private native static int getScreenHeight()/*-{
		return $wnd.screen.height;
    }-*/;

    private void send(MessageType type, JavaScriptObject data) {
        boolean messageSent = false;
        if (remoteWindow != null) {
            if (remoteWindow.canSend()) {
                remoteWindow.sendMessage(type, data);
                messageSent = true;
            }
            if (!remoteWindow.isOpen()) {
                openRemoteWindow();
            }
        } else {
            openRemoteWindow();
        }

        if (!messageSent) {
            pendingMessages.add(new PendingMessage(type, data));
        }
    }

    public void log(String msg) {
        log(msg, false);
    }

    private void log(String msg, boolean isError) {
        String location = getCallLocation();
        send(MessageType.LOG_MESSAGE,
                LogMessage.make(msg, isError, location, consoleId));
        WindowDebugConsole.logToConsole(msg);
    }

    private String getCallLocation() {
        if (!GWT.isProdMode()) {
            Throwable t = new Throwable();
            StackTraceElement[] stackTrace = t.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!className.equals(getClass().getName())
                        && !className
                                .equals(WindowDebugConsole.class.getName())
                        && !className.equals(VConsole.class.getName())) {
                    return "at " + element.getClassName() + "."
                            + element.getMethodName() + "("
                            + element.getFileName() + ":"
                            + element.getLineNumber() + ")";
                }
            }
        }
        return null;
    }

    private void log(Throwable e, boolean isError) {
        if (e instanceof UmbrellaException) {
            UmbrellaException umbrella = (UmbrellaException) e;
            for (Throwable throwable : umbrella.getCauses()) {
                log(throwable);
            }
            return;
        }
        log(Util.getSimpleName(e) + ": " + e.getMessage(), isError);
        GWT.log(e.getMessage(), e);
    }

    public void log(Throwable e) {
        log(e, false);
    }

    public void error(Throwable e) {
        log(e, true);
    }

    public void error(String msg) {
        log(msg, true);
    }

    public void printObject(Object msg) {
        String str;
        if (msg == null) {
            str = "null";
        } else {
            str = msg.toString();
        }
        log(str, false);
    }

    public void dirUIDL(ValueMap u, ApplicationConfiguration cnf) {

        send(MessageType.UIDL, UIDLMessage.createMessage(u, cnf, consoleId));
    }

    public void printLayoutProblems(ValueMap meta,
            ApplicationConnection applicationConnection,
            Set<Paintable> zeroHeightComponents,
            Set<Paintable> zeroWidthComponents) {
        send(MessageType.LAYOUT_PROBLEMS, LayoutProblemMessage.createMessage(
                meta, applicationConnection, zeroHeightComponents,
                zeroWidthComponents, consoleId));
    }

    public void onMessage(String typeString, JavaScriptObject data,
            RemoteWindow source) {
        MessageType type = MessageType.valueOf(typeString);
        switch (type) {
        case ANNOUNCE:
            remoteWindow = source;
            flushQueue();
            break;
        case RESTART:
            restartApplication();
            break;
        case ANALYZE_LAYOUTS:
            analyzeLayouts();
            break;
        case SELECT:
            SelectMessage message = data.cast();
            select(message);
            break;
        case SELECT_START:
            Element label = data.cast();
            highlightModeRegistration = Event
                    .addNativePreviewHandler(new HighlightModeHandler(label));
            break;
        case DEVMODE:
            boolean enable = data != null;
            if (enable) {
                enableDevMode();
            } else {
                disableDevMode();
            }
            break;
        default:
            WindowDebugConsole
                    .logToConsole("Unsupported message type: " + type);
            break;
        }
    }

    static Element highlight = Document.get().createDivElement().cast();

    static {
        Style style = highlight.getStyle();
        style.setPosition(Position.ABSOLUTE);
        style.setZIndex(VWindow.Z_INDEX + 1000);
        style.setBackgroundColor("red");
        style.setOpacity(0.2);
        if (BrowserInfo.get().isIE()) {
            style.setProperty("filter", "alpha(opacity=20)");
        }
    }

    static void highlight(Paintable paintable) {
        Widget w = (Widget) paintable;
        if (w != null) {
            Style style = highlight.getStyle();
            style.setTop(w.getAbsoluteTop(), Unit.PX);
            style.setLeft(w.getAbsoluteLeft(), Unit.PX);
            style.setWidth(w.getOffsetWidth(), Unit.PX);
            style.setHeight(w.getOffsetHeight(), Unit.PX);
            RootPanel.getBodyElement().appendChild(highlight);
        }
    }

    static void deHiglight() {
        if (highlight.getParentElement() != null) {
            highlight.getParentElement().removeChild(highlight);
        }
    }

    private void select(SelectMessage message) {
        List<ApplicationConnection> runningApplications = ApplicationConfiguration
                .getRunningApplications();
        String pid = message.getPid();
        if (pid == null) {
            deHiglight();
        } else {

            // TODO this does not work properly with multiple application on
            // same
            // host page
            for (ApplicationConnection applicationConnection : runningApplications) {
                Paintable paintable = applicationConnection.getPaintable(pid);
                highlight(paintable);
                if (message.isOnServer()) {
                    PackageDelegate.highlightOnServer(applicationConnection,
                            paintable);
                }
            }
        }
    }

    private void analyzeLayouts() {
        List<ApplicationConnection> runningApplications = ApplicationConfiguration
                .getRunningApplications();
        for (ApplicationConnection applicationConnection : runningApplications) {
            applicationConnection.analyzeLayouts();
        }
    }

    private void disableDevMode() {
        if (Window.Location.getParameter("gwt.codesvr") == null) {
            // Not enabled - nothing to disable
            return;
        }
        String location = Window.Location.getHref();
        String pattern = "\\&?gwt\\.codesvr=[^&]*";
        if (Window.Location.getQueryString().startsWith("?gwt.codesvr")) {
            pattern += "\\&?";
        }
        location = location.replaceFirst(pattern, "");
        Window.Location.replace(location);
    }

    private void enableDevMode() {
        if (Window.Location.getParameter("gwt.codesvr") != null) {
            // Already active - nothing to enable
            return;
        } else {
            addUrlParameter("gwt.codesvr=" + Location.getHostName() + ":9997");
        }
    }

    private void restartApplication() {
        String parameterName = "restartApplication";
        if (Window.Location.getParameter(parameterName) != null) {
            Window.Location.reload();
        } else {
            addUrlParameter(parameterName);
        }
    }

    private void addUrlParameter(String parameterName) {
        String url = Window.Location.getHref();
        if (url.contains("?")) {
            url += '&';
        } else {
            url += '?';
        }
        url += parameterName;
        if (!"".equals(Location.getHash())) {
            String hash = Location.getHash();
            url = url.replace(hash, "") + hash;
        }
        Window.Location.replace(url);
    }

    private void flushQueue() {
        ArrayList<PendingMessage> messages = new ArrayList<PendingMessage>(
                pendingMessages);
        pendingMessages.clear();

        sendInit();
        for (PendingMessage p : messages) {
            if (p.type != MessageType.ANNOUNCE && p.type != MessageType.INIT) {
                send(p.type, p.data);
            }
        }
    }

    private static String ensureWindowName() {
        String windowName = WindowDebugConsole.getWindowName();
        if (windowName == null || windowName.length() == 0) {
            int id = Random.nextInt();
            if (id < 0) {
                id = -id;
            }
            windowName = Integer.toString(id);
            setWindowName(windowName);
        }
        return windowName;
    }

    private static native void setWindowName(String name) /*-{
		$wnd.name = name;
    }-*/;

}
