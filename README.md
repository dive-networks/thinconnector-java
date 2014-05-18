##thinconnector-java

A thin connector for the Gnip API written in Java. The connector uses maven to manage the few dependencies that it has
including FasterXML's [Jackson libs for JSON parsing](https://github.com/FasterXML/jackson).

###Features
* Logging
* Connection to a stream with reconnect logic
* Rules management
* Use of properties
* Asynchronous reading off the stream, parsing JSON and handling of messages

##Coming
* Metrics
* Other streams besides PowerTrack

##Usage
(from the root directory of the repository)

    cp src/main/resources/example.config.properties src/main/resources/config.properties

Edit config.properties and fill in your own Gnip connection values.

Then build the app: `mvn clean package`.

Start the Mongo daemon if you are using the MongoStreamHandler: `mongod &`.
Messages will be written into your database as they arrive from the stream.

Now run the app:

    java -cp target/thinconnector-java-1.0.jar com.gnip.IngesteratorApplication

You should see logger messages written to stdout.