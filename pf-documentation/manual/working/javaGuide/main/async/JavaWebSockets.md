<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# WebSockets

[WebSockets](http://en.wikipedia.org/wiki/WebSocket) are sockets that can be used from a web browser based on a protocol that allows two way full duplex communication.  The client can send messages and the server can receive messages at any time, as long as there is an active WebSocket connection between the server and the client.

Modern HTML5 compliant web browsers natively support WebSockets via a JavaScript WebSocket API.  However WebSockets are not limited in just being used by WebBrowsers, there are many WebSocket client libraries available, allowing for example servers to talk to each other, and also native mobile apps to use WebSockets.  Using WebSockets in these contexts has the advantage of being able to reuse the existing TCP port that a Play server uses.

## Handling WebSockets

Until now, we've been writing methods that return `Result` to handle standard HTTP requests.  WebSockets are quite different and can’t be handled via standard Play actions.

Play provides two different built in mechanisms for handling WebSockets.  The first is using actors, the second is using simple callbacks.  Both of these mechanisms can be accessed using the builders provided on [WebSocket](api/java/play/mvc/WebSocket.html).

## Handling WebSockets with actors

To handle a WebSocket with an actor, we need to give Play a `akka.actor.Props` object that describes the actor that Play should create when it receives the WebSocket connection.  Play will give us an `akka.actor.ActorRef` to send upstream messages to, so we can use that to help create the `Props` object:

Java 8
: @[imports](java8code/java8guide/async/JavaWebSockets.java)
@[actor-accept](java8code/java8guide/async/JavaWebSockets.java)

Java
: @[imports](code/javaguide/async/JavaWebSockets.java)
@[actor-accept](code/javaguide/async/JavaWebSockets.java)

The actor that we're sending to here in this case looks like this:

@[actor](code/javaguide/async/MyWebSocketActor.java)

Any messages received from the client will be sent to the actor, and any messages sent to the actor supplied by Play will be sent to the client.  The actor above simply sends every message received from the client back with `I received your message: ` prepended to it.

### Detecting when a WebSocket has closed

When the WebSocket has closed, Play will automatically stop the actor.  This means you can handle this situation by implementing the actors `postStop` method, to clean up any resources the WebSocket might have consumed.  For example:

@[actor-post-stop](code/javaguide/async/JavaWebSockets.java)

### Closing a WebSocket

Play will automatically close the WebSocket when your actor that handles the WebSocket terminates.  So, to close the WebSocket, send a `PoisonPill` to your own actor:

@[actor-stop](code/javaguide/async/JavaWebSockets.java)

### Rejecting a WebSocket

Sometimes you may wish to reject a WebSocket request, for example, if the user must be authenticated to connect to the WebSocket, or if the WebSocket is associated with some resource, whose id is passed in the path, but no resource with that id exists.  Play provides a `reject` WebSocket builder for this purpose:

Java 8
: @[actor-reject](java8code/java8guide/async/JavaWebSockets.java)

Java
: @[actor-reject](code/javaguide/async/JavaWebSockets.java)

### Accepting a WebSocket asynchronously

You may need to do some asynchronous processing before you are ready to create an actor or reject the WebSocket, if that's the case, you can simply return `Promise<WebSocket<A>>` instead of `WebSocket<A>`.

### Handling different types of messages

So far we have only seen handling `String` frames.  Play also has built in handlers for `byte[]` frames, and `JSONNode` messages parsed from `String` frames.  You can pass these as the type parameters to the WebSocket creation method, for example:

Java 8
: @[actor-json](java8code/java8guide/async/JavaWebSockets.java)

Java
: @[actor-json](code/javaguide/async/JavaWebSockets.java)

## Handling WebSockets using callbacks

If you don't want to use actors to handle a WebSocket, you can also handle it using simple callbacks.

To handle a WebSocket your method must return a `WebSocket` instead of a `Result`:

Java 8
: @[websocket](java8code/java8guide/async/JavaWebSockets.java)

Java
: @[websocket](code/javaguide/async/JavaWebSockets.java)

A WebSocket has access to the request headers (from the HTTP request that initiates the WebSocket connection) allowing you to retrieve standard headers and session data. But it doesn't have access to any request body, nor to the HTTP response.

When the `WebSocket` is ready, you get both `in` and `out` channels.

It this example, we print each message to console and we send a single **Hello!** message.

> **Tip:** You can test your WebSocket controller on <http://websocket.org/echo.html>. Just set the location to `ws://localhost:9000`.

Let’s write another example that totally discards the input data and closes the socket just after sending the **Hello!** message:

Java 8
: @[discard-input](java8code/java8guide/async/JavaWebSockets.java)

Java
: @[discard-input](code/javaguide/async/JavaWebSockets.java)
