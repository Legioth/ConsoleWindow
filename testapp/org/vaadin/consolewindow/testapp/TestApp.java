package org.vaadin.consolewindow.testapp;

import com.vaadin.Application;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class TestApp extends Application {

    @Override
    public void init() {
        final Window mainWindow = new Window("TestApp");

        mainWindow.addComponent(new Label("Just the main window here"));
        mainWindow.addComponent(new Button("Click to sync",
                new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        mainWindow
                                .addComponent(new Label("Button was clicked"));
                    }
                }));

        Label label = new Label("Invalid height");
        label.setHeight("100%");
        mainWindow.addComponent(label);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setHeight("10px");

        Label label2 = new Label("Zero height");
        label2.setHeight("100%");
        verticalLayout.addComponent(label2);
        verticalLayout.addComponent(new Label("Just a filler"));

        mainWindow.addComponent(verticalLayout);

        setMainWindow(mainWindow);
    }
}
