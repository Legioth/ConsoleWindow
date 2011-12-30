package org.vaadin.consolewindow.client.message;

import java.util.Set;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Container;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.ValueMap;

public final class LayoutProblemMessage extends JavaScriptObject {

    protected LayoutProblemMessage() {
        // JSO constructor
    }

    public native ValueMap getValueMap() /*-{
		return this.valueMap;
    }-*/;

    public static LayoutProblemMessage createMessage(ValueMap meta,
            ApplicationConnection ac, Set<Paintable> zeroHeightComponents,
            Set<Paintable> zeroWidthComponents, int consoleId) {
        JsArray<ValueMap> valueMapArray = meta.getValueMap("invalidLayouts")
                .cast();
        for (int i = 0; i < valueMapArray.length(); i++) {
            complementValueMap(valueMapArray.get(i), ac);
        }

        JsArray<ZeroSizePaintable> zeroHeightArray = processZeroSizeSet(zeroHeightComponents);
        JsArray<ZeroSizePaintable> zeroWidthArray = processZeroSizeSet(zeroWidthComponents);

        return createMessage(meta, consoleId, zeroHeightArray, zeroWidthArray);
    }

    private static JsArray<ZeroSizePaintable> processZeroSizeSet(
            Set<Paintable> zeroSizeComponents) {
        JsArray<ZeroSizePaintable> array = JavaScriptObject.createArray()
                .cast();
        for (Paintable paintable : zeroSizeComponents) {
            final Container layout = Util.getLayout((Widget) paintable);

            String paintableName = Util.getSimpleName(paintable);
            String layoutName = Util.getSimpleName(layout);
            Element layoutElement = ((Widget) layout).getElement();
            array.push(ZeroSizePaintable.create(paintableName, layoutName,
                    layoutElement));
        }

        return array;
    }

    public native JsArray<ZeroSizePaintable> getZeroHeight() /*-{
		return this.zeroHeight;
    }-*/;

    public native JsArray<ZeroSizePaintable> getZeroWidth() /*-{
		return this.zeroWidth;
    }-*/;

    private native static LayoutProblemMessage createMessage(ValueMap meta,
            int consoleId, JsArray<ZeroSizePaintable> zeroHeightArray,
            JsArray<ZeroSizePaintable> zeroWidthArray) /*-{
		return {
			valueMap : meta,
			console : consoleId,
			zeroHeight : zeroHeightArray,
			zeroWidth : zeroWidthArray
		};
    }-*/;

    private static void complementValueMap(ValueMap valueMap,
            ApplicationConnection ac) {
        final String pid = valueMap.getString("id");
        final Paintable paintable = ac.getPaintable(pid);
        String name = Util.getSimpleName(paintable);
        Element element = ((Widget) paintable).getElement();
        addExtendedValues(valueMap, name, element);

        if (valueMap.containsKey("subErrors")) {
            JsArray<ValueMap> suberrors = valueMap.getValueMap("subErrors")
                    .cast();
            for (int i = 0; i < suberrors.length(); i++) {
                ValueMap value = suberrors.get(i);
                complementValueMap(value, ac);
            }
        }
    }

    private native static void addExtendedValues(ValueMap valueMap,
            String name, Element element)/*-{
		valueMap.simpleName = name;
		valueMap.remoteElement = element;
    }-*/;

    public native int getConsole() /*-{
		return this.console;
    }-*/;

}
