package org.vaadin.consolewindow.client.message;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;

public final class ZeroSizePaintable extends JavaScriptObject {
    protected ZeroSizePaintable() {
        // JSO constructor
    }

    public native String getPaintableName() /*-{
		return this.paintableName;
    }-*/;

    public native String getLayoutName() /*-{
		return this.layoutName;
    }-*/;

    public native Element getLayoutElement() /*-{
		return this.layoutElement;
    }-*/;

    public native static ZeroSizePaintable create(String paintableName,
            String layoutName, Element layoutElement) /*-{
		return {
			paintableName : paintableName,
			layoutName : layoutName,
			layoutElement : layoutElement
		}
    }-*/;

}
