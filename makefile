JFLAGS=-Xlint:unchecked

all: client ws
client: Client.java Data.java Metadata.java
	javac *.java $(JFLAGS)

ws:	broadcast/*.java
	javac broadcast/*.java

clean:
	rm *.class;rm broadcast/*.class
