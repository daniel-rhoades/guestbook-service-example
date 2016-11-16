# Guestbook Service (example)

An example microservice written in Scala exposed using the Akka toolkit connected to a MongoDB database.

The solution has been designed to address the following challenge:

> Provide a service which can accept a person's first name and last name along with a comment (e.g. hotel guestbook).  The service should be exposed as REST API which is asynchronous and non-blocking.

It comprises of the following sub-components:

* Scala application which prints the ubiquitous "Hello World";
* Akka HTTP service wrapping the above application;
* Unit tests using ScalaTest.

The concept of this service is built on [greeter-service-example](https://github.com/daniel-rhoades/greeter-service-example).

## Design

### Interface

The interface will accept a single POST request of JSON data to the `/guestbook` resource.  A `GuestbookRequest` will comprise of a person's first and last names along with their comment as separate key/value string pairs.  A valid request though should look like this as an example:

```
$ curl -X POST -H 'Content-Type: application/json' http://localhost:9000/guestbook -d '{"firstName": "Bob", "lastName": "Smith", "comment": "Oh what a wonderful place"}'
```

The expected `GuestbookResponse` should also be in JSON format, with a message reporting that the person's comment has been recorded:

```
{
  "message": "Your comment has been recorded"
}
```

### Message Model

The `GuestbookRequest` and `GuestbookResponse` messages will be interally modelled as case cases based on interface specification above.  The application will define implicit variables to handle the marshalling/unmarshalling. 

### Application Logic

The actual application logic will be modelled as the object `GuestbookLogic` where it persist the comment information into a database.

### Service

A `Service` trait will provide the routing and request handling logic.  It will define an Akka HTTP route for handling requests based on the interface specification.  The `Service` will wrap calls to `GuestbookLogic` in a Future to enable asynchronous operation using the Actor model.  It should either return the `GuestbookResponse` or an error message with a 400 status.
 
The `AkkaHttpMicroservice` will operate as the executable App extending the `Service` and using the Akka HTTP toolkit (streams and http DSL) to bind the App to a configurable IP address/port.  The default request timeout (20 seconds) is acceptable.

### Platform

The packaged application will be deployed as a [Docker](https://docs.docker.com/engine/understanding-docker/) container, the database will be an instance of [MongoDB](https://www.mongodb.com).

## Building and Testing

Firstly, install and run a local version of MongoDB.

SBT is used to build the project:

```
$ sbt
> compile
```

ScalaTest Specifications are used to unit test at the Service level:

```
$ sbt
> test
```

The application can be started using the [Spray sbt-revolver plugin](https://github.com/spray/sbt-revolver):

```
$ export GUESTBOOK_DATABASE_URI="mongodb://localhost"
$ sbt ~re-start
```

The `~` will cause the plugin to enter "triggered restart" so that any changes to the source code will be picked up and the service restarted.  This is extremely useful for rapid development.

## Packaging

To build a "fat" JAR with all the dependencies:

```
$ sbt assembly
```

The project also has a Docker file based on [Alpine Linux](http://www.alpinelinux.org/) (a lightweight, security-oriented Linux distro), built around muslc libc and busybox it is only ~130MB in size.  The standard [Oracle JRE 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) is also used.  Even better, somebody has already put this all together as a Docker image [frolvlad/alpine-oraclejdk8](https://hub.docker.com/r/frolvlad/alpine-oraclejdk8/) weighing in ~170MB, so our Dockerfile is built on top of that.

To build the Docker image:

```
$ docker build -t guestbook-service-example .
```

Total size of all the Docker layers is ~190MB - which less than a full Oracle JDK download.

## Running

Firstly, ensure MongoDB is running locally.

To run the packaged "fat" JAR:

```
$ java -jar target/scala-2.11/guestbook-service-example-assembly-0.1.jar
```

Or if you have built the Docker image:

```
$ docker run guestbook-service-example -p 9000:9000
```

At this time is is defaulting to use a local MongoDB instance, this can be changed by specifying a connection URI as an environment variable:

```
$ export DATABASE_URI="<my-uri>"
```

If the URI specifies an SSL connection, then a client will be created using support from [Netty](http://mongodb.github.io/mongo-scala-driver/1.0/reference/connecting/ssl/). 