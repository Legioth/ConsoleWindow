package org.vaadin.consolewindow.client.message;

import java.util.Iterator;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.vaadin.terminal.gwt.client.ApplicationConfiguration;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.ValueMap;
import com.vaadin.terminal.gwt.client.ui.VUnknownComponent;
import com.vaadin.terminal.gwt.client.ui.VView;

public final class UIDLMessage extends JavaScriptObject {
    protected UIDLMessage() {
        // JSO constructor
    }

    public native ValueMap getValueMap() /*-{
		return this.uidl;
    }-*/;

    public native int getConsole() /*-{
		return this.console;
    }-*/;

    public native String getWidgetName(int pid)/*-{
		return this.names[pid];
    }-*/;

    public static UIDLMessage createMessage(ValueMap map,
            ApplicationConfiguration conf, int consoleId) {
        JsArrayString widgetNames = JsArrayString.createArray().cast();

        Set<String> keySet = map.getKeySet();
        for (String key : keySet) {
            if (key.equals("changes")) {
                JsArray<UIDL> jsValueMapArray = map.getValueMap("changes")
                        .cast();
                for (int i = 0; i < jsValueMapArray.length(); i++) {
                    UIDL uidl = jsValueMapArray.get(i);
                    populateWidgetNames(uidl, conf, widgetNames);
                }
            }
        }

        return createMessage(map, consoleId, widgetNames);
    }

    private static void populateWidgetNames(UIDL uidl,
            ApplicationConfiguration conf, JsArrayString widgetNames) {
        try {
            String tag = uidl.getTag();
            int tagId = Integer.parseInt(tag);
            String name = getNodeName(uidl, conf, tag);
            widgetNames.set(tagId, name);

        } catch (Exception e) {
            // NOP
        }

        final Iterator<Object> i = uidl.getChildIterator();
        while (i.hasNext()) {
            final Object child = i.next();
            if (child instanceof UIDL) {
                final UIDL c = (UIDL) child;
                populateWidgetNames(c, conf, widgetNames);
            }
        }
    }

    private static String getNodeName(UIDL uidl, ApplicationConfiguration conf,
            String name) {
        Class<? extends Paintable> widgetClassByDecodedTag = conf
                .getWidgetClassByEncodedTag(name);
        if (widgetClassByDecodedTag == VUnknownComponent.class) {
            return "(NO CLIENT IMPLEMENTATION FOUND)";
        } else if (widgetClassByDecodedTag == VView.class
                && uidl.hasAttribute("sub")) {
            return "com.vaadin.terminal.gwt.ui.VWindow";
        } else {
            return widgetClassByDecodedTag.getName();
        }
    }

    private static native UIDLMessage createMessage(ValueMap map,
            int consoleId, JsArrayString widgetNames) /*-{
		return {
			uidl : map,
			console : consoleId,
			names : widgetNames
		};
    }-*/;
}
