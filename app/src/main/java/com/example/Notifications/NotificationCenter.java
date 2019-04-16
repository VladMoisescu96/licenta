package com.example.Notifications;

import java.util.Set;
import java.util.WeakHashMap;

public class NotificationCenter {

    private WeakHashMap<String, WeakHashMap<HandlerInterface, Boolean>> eventsMap;
    private static NotificationCenter eventDispatcher;

    private NotificationCenter() {

        eventsMap = new WeakHashMap<>();
    }

    public static NotificationCenter getInstance() {

        if (eventDispatcher == null) {
            eventDispatcher = new NotificationCenter();
        }
        return eventDispatcher;
    }

    public synchronized void on(String eventName, HandlerInterface selector) {

        if (!eventsMap.containsKey(eventName)) {

            WeakHashMap<HandlerInterface, Boolean> handlersMap = new WeakHashMap<>();
            handlersMap.put(selector, Boolean.TRUE);
            eventsMap.put(eventName, handlersMap);
        } else {

            eventsMap.get(eventName).put(selector, Boolean.TRUE);
        }
    }

    public synchronized void emit(String eventName, String payload) {

        if (eventsMap.containsKey(eventName)) {
            for (HandlerInterface handlerInterface : eventsMap.get(eventName).keySet()) {

                handlerInterface.handler(payload);
            }
        }

    }

    public synchronized void off(String eventName, HandlerInterface selector) {

        if (eventsMap.containsKey(eventName)) {
            eventsMap.get(eventName).remove(selector);
        }
    }

    public synchronized HandlerInterface[] getListenersByEventName(String eventName) {

        if (eventsMap.containsKey(eventName)) {
            Set handlersSet = eventsMap.get(eventName).keySet();
            return (HandlerInterface[]) handlersSet.toArray(new HandlerInterface[handlersSet.size()]);
        }
        return null;
    }
}
