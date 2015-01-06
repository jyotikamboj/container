/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.advanced.di;

public class MessageQueue {
    public static boolean stopped = false;

    public static MessageQueue connect() {
        return new MessageQueue();
    }

    public void stop() {
        stopped = true;
    }
}
