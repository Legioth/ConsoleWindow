package org.vaadin.consolewindow.client;

import java.util.Date;

import org.vaadin.consolewindow.client.message.InitMessage;
import org.vaadin.consolewindow.client.message.LayoutProblemMessage;
import org.vaadin.consolewindow.client.message.MessageType;
import org.vaadin.consolewindow.client.message.SelectMessage;
import org.vaadin.consolewindow.client.message.LogMessage;
import org.vaadin.consolewindow.client.message.UIDLMessage;
import org.vaadin.consolewindow.client.message.ZeroSizePaintable;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.SimpleTree;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VCustomUIDLBrowser;
import com.vaadin.terminal.gwt.client.ValueMap;

public class ConsoleSession extends SimpleTree {
    private static final DateTimeFormat timeFormat = DateTimeFormat
            .getFormat(PredefinedFormat.HOUR24_MINUTE_SECOND);

    private final JsDate startTime;

    private boolean noMessages = true;

    private final RemoteWindow remoteWindow;

    public ConsoleSession(InitMessage initMessage, RemoteWindow remoteWindow) {
        this.remoteWindow = remoteWindow;
        startTime = initMessage.getTime();
        setStyleName("consoleSession");
        setText(timeFormat.format(new Date((long) startTime.getTime())) + " "
                + initMessage.getLocation());
        addItem("");
    }

    public void log(LogMessage message) {
        if (noMessages) {
            remove(0);
            // getChild(0).remove();
            noMessages = false;
        }
        String text = message.getMessage();

        text = "+"
                + NumberFormat.getFormat("0.000")
                        .format((message.getTime().getTime() - startTime
                                .getTime()) / 1000) + "s " + text;

        SimpleTree item = new SimpleTree(text);
        // TreeItem item = addItem(text);
        String location = message.getLocation();
        if (location != null) {
            item.setTitle(location);
        }
        if (message.isError()) {
            item.addStyleName("error");
        }
        add(item);
        open(false);
        // setState(true);
    }

    public void logUIDL(final UIDLMessage uidlMessage) {
        VCustomUIDLBrowser vuidlBrowser = new VCustomUIDLBrowser(
                uidlMessage.getValueMap()) {
            @Override
            protected String getNodeName(UIDL uidl, String name) {
                try {
                    int pid = Integer.parseInt(name);
                    return uidlMessage.getWidgetName(pid);
                } catch (Exception e) {
                    return name;
                }
            }

            @Override
            protected void select(String paintableId, boolean selectOnServer) {
                remoteWindow.sendMessage(MessageType.SELECT,
                        SelectMessage.create(paintableId, selectOnServer));
            }
        };
        add(vuidlBrowser);
        vuidlBrowser.setText("UIDL response");
        open(false);
    }

    public void showLayoutProblems(LayoutProblemMessage layoutMessage) {
        ValueMap meta = layoutMessage.getValueMap();
        JsArray<ValueMap> valueMapArray = meta.getValueMap("invalidLayouts")
                .cast();
        int size = valueMapArray.length();
        add(new HTML("<div>************************</di>"
                + "<h4>Layouts analyzed on server, total top level problems: "
                + size + " </h4>"));
        if (size > 0) {
            SimpleTree root = new SimpleTree("Root problems");

            for (int i = 0; i < size; i++) {
                printLayoutError(valueMapArray.get(i), root);
            }
            add(root);
        }

        JsArray<ZeroSizePaintable> zeroHeight = layoutMessage.getZeroHeight();
        JsArray<ZeroSizePaintable> zeroWidth = layoutMessage.getZeroWidth();

        if (zeroHeight.length() > 0 || zeroWidth.length() > 0) {
            add(new HTML("<h4> Client side notifications</h4>"
                    + " <em>The following relative sized components were "
                    + "rendered to a zero size container on the client side."
                    + " Note that these are not necessarily invalid "
                    + "states, but reported here as they might be.</em>"));
            if (zeroHeight.length() > 0) {
                add(new HTML("<p><strong>Vertically zero size:</strong><p>"));
                printClientSideDetectedIssues(zeroHeight);
            }
            if (zeroWidth.length() > 0) {
                add(new HTML("<p><strong>Horizontally zero size:</strong><p>"));
                printClientSideDetectedIssues(zeroWidth);
            }
        }
        add(new Label("************************"));

    }

    private void printClientSideDetectedIssues(
            JsArray<ZeroSizePaintable> paintables) {
        for (int i = 0; i < paintables.length(); i++) {
            final ZeroSizePaintable zeroSizePaintable = paintables.get(i);

            VerticalPanel errorDetails = new VerticalPanel();
            errorDetails.add(new Label(""
                    + zeroSizePaintable.getPaintableName() + " inside "
                    + zeroSizePaintable.getLayoutName()));
            final CheckBox emphasisInUi = new CheckBox(
                    "Emphasize components parent in UI (the actual component is not visible)");
            emphasisInUi.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    Element element2 = zeroSizePaintable.getLayoutElement();
                    Widget.setStyleName(element2, "invalidlayout",
                            emphasisInUi.getValue());
                }
            });
            errorDetails.add(emphasisInUi);
            add(errorDetails);
        }
    }

    private void printLayoutError(final ValueMap valueMap, SimpleTree root) {
        final String pid = valueMap.getString("id");

        SimpleTree errorNode = new SimpleTree();
        VerticalPanel errorDetails = new VerticalPanel();
        errorDetails.add(new Label(valueMap.getString("simpleName") + " id: "
                + pid));
        if (valueMap.containsKey("heightMsg")) {
            errorDetails.add(new Label("Height problem: "
                    + valueMap.getString("heightMsg")));
        }
        if (valueMap.containsKey("widthMsg")) {
            errorDetails.add(new Label("Width problem: "
                    + valueMap.getString("widthMsg")));
        }
        final CheckBox emphasisInUi = new CheckBox("Emphasize component in UI");
        emphasisInUi.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Element element = valueMap.getValueMap("remoteElement").cast();
                Widget.setStyleName(element, "invalidlayout",
                        emphasisInUi.getValue());
            }
        });
        errorDetails.add(emphasisInUi);
        errorNode.add(errorDetails);
        if (valueMap.containsKey("subErrors")) {
            HTML l = new HTML(
                    "<em>Expand this node to show problems that may be dependent on this problem.</em>");
            errorDetails.add(l);
            JsArray<ValueMap> suberrors = valueMap.getValueMap("subErrors")
                    .cast();
            for (int i = 0; i < suberrors.length(); i++) {
                ValueMap value = suberrors.get(i);
                printLayoutError(value, errorNode);
            }

        }
        root.add(errorNode);
    }
}
