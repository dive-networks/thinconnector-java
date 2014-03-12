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