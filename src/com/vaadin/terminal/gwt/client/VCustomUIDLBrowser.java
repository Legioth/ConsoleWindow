/*
 * Copyright 2011 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * 
 */
package com.vaadin.terminal.gwt.client;

import java.util.Iterator;
import java.util.Set;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;

public abstract class VCustomUIDLBrowser extends SimpleTree {
    private static final String HELP = "Shift click handle to open recursively. Click components to hightlight them on client side. Shift click components to highlight them also on the server side.";
    private String highlightedPid;
    private boolean hasHoverHighlight = false;

    public VCustomUIDLBrowser(final UIDL uidl) {
        final UIDLItem root = new UIDLItem(uidl);
        add(root);
    }

    public VCustomUIDLBrowser(ValueMap u) {
        ValueMap valueMap = u.getValueMap("meta");
        if (valueMap.containsKey("hl")) {
            highlightedPid = valueMap.getString("hl");
        }
        Set<String> keySet = u.getKeySet();
        for (String key : keySet) {
            if (key.equals("changes")) {
                JsArray<UIDL> jsValueMapArray = u.getJSValueMapArray("changes")
                        .cast();
                for (int i = 0; i < jsValueMapArray.length(); i++) {
                    UIDL uidl = jsValueMapArray.get(i);
                    UIDLItem change = new UIDLItem(uidl);
                    change.setTitle("change " + i);
                    add(change);
                }
            } else if (key.equals("meta")) {

            } else {
                // TODO consider pretty printing other request data
                // addItem(key + " : " + u.getAsString(key));
            }
        }
        open(highlightedPid != null);
        setTitle(HELP);
    }

    class UIDLItem extends SimpleTree {

        private UIDL uidl;

        UIDLItem(UIDL uidl) {
            setTitle(HELP);
            this.uidl = uidl;
            try {
                String name = uidl.getTag();
                try {
                    Integer.parseInt(name);
                    name = getNodeName(uidl, name);
                } catch (Exception e) {
                    // NOP
                }
                setText(name);
                addItem("LOADING");
            } catch (Exception e) {
                setText(uidl.toString());
            }

            addDomHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent event) {
                    if (hasHoverHighlight) {
                        hasHoverHighlight = false;
                        VCustomUIDLBrowser.this.select(null, false);
                    }
                }
            }, MouseOutEvent.getType());

        }

        @Override
        public void open(boolean recursive) {
            if (getWidgetCount() == 1
                    && getWidget(0).getElement().getInnerText()
                            .equals("LOADING")) {
                dir();
            }
            super.open(recursive);
        }

        @Override
        protected void select(ClickEvent event) {
            boolean selectOnServer = event != null
                    && event.getNativeEvent().getShiftKey();
            String paintableId = uidl.getId();
            hasHoverHighlight = true;
            VCustomUIDLBrowser.this.select(paintableId, selectOnServer);

            super.select(event);
        }

        public void dir() {
            remove(0);

            String nodeName = uidl.getTag();
            try {
                Integer.parseInt(nodeName);
                nodeName = getNodeName(uidl, nodeName);
            } catch (Exception e) {
                // NOP
            }

            Set<String> attributeNames = uidl.getAttributeNames();
            for (String name : attributeNames) {
                if (uidl.isMapAttribute(name)) {
                    try {
                        ValueMap map = uidl.getMapAttribute(name);
                        JsArrayString keyArray = map.getKeyArray();
                        nodeName += " " + name + "=" + "{";
                        for (int i = 0; i < keyArray.length(); i++) {
                            nodeName += keyArray.get(i) + ":"
                                    + map.getAsString(keyArray.get(i)) + ",";
                        }
                        nodeName += "}";
                    } catch (Exception e) {

                    }
                } else {
                    final String value = uidl.getAttribute(name);
                    nodeName += " " + name + "=" + value;
                }
            }
            setText(nodeName);

            try {
                SimpleTree tmp = null;
                Set<String> variableNames = uidl.getVariableNames();
                for (String name : variableNames) {
                    String value = "";
                    try {
                        value = uidl.getVariable(name);
                    } catch (final Exception e) {
                        try {
                            String[] stringArrayAttribute = uidl
                                    .getStringArrayAttribute(name);
                            value = stringArrayAttribute.toString();
                        } catch (final Exception e2) {
                            try {
                                final int intVal = uidl.getIntVariable(name);
                                value = String.valueOf(intVal);
                            } catch (final Exception e3) {
                                value = "unknown";
                            }
                        }
                    }
                    if (tmp == null) {
                        tmp = new SimpleTree("variables");
                    }
                    tmp.addItem(name + "=" + value);
                }
                if (tmp != null) {
                    add(tmp);
                }
            } catch (final Exception e) {
                // Ignored, no variables
            }

            final Iterator<Object> i = uidl.getChildIterator();
            while (i.hasNext()) {
                final Object child = i.next();
                try {
                    final UIDL c = (UIDL) child;
                    final UIDLItem childItem = new UIDLItem(c);
                    add(childItem);

                } catch (final Exception e) {
                    addItem(child.toString());
                }
            }
            if (highlightedPid != null && highlightedPid.equals(uidl.getId())) {
                getElement().getStyle().setBackgroundColor("#fdd");
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    public void execute() {
                        getElement().scrollIntoView();
                    }
                });
            }
        }
    }

    protected abstract String getNodeName(UIDL uidl, String name);

    protected abstract void select(String paintableId, boolean selectOnServer);

}
