package com.vaadin.terminal.gwt.client;

/**
 * Static helper just to access some methods that are package private.
 */
public class PackageDelegate {
    public static void highlightOnServer(
            ApplicationConnection applicationConnection, Paintable paintable) {
        applicationConnection.highlightComponent(paintable);
    }
}
