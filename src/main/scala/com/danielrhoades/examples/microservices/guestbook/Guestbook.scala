package com.danielrhoades.examples.microservices.guestbook

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.mongodb.ConnectionString
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.ClusterSettings
import com.typesafe.config.{Config, ConfigFactory}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.collection.immutable.Document
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.mongodb.scala.connection._

/**
  * Service request for a guestbook message
  *
  * @param firstName
  * @param lastName
  */
case class GuestbookRequest(firstName: String, lastName: String, comment: String)

/**
  * Service response for a guestbook message
  *
  * @param message
  */
case class GuestbookResponse(message: String)

/**
  * Business logic that will determine the guestbook message
  */
class GuestbookLogic(implicit ec: ExecutionContext) {

  val config: Config = ConfigFactory.load()

  implicit val system = ActorSystem()
  implicit val logger = Logging(system, getClass)
  implicit val timeout: Timeout = 5.seconds

  val client: MongoClient = {

    val uri = config.getString("services.database-uri")
    logger.debug(s"Creating MongoClient for URI: $uri")

    uri match {
      case ssl if (ssl.contains("ssl")) =>
        logger.debug("Creating SSL MongoClientSettings")
        val connectionString = new ConnectionString(uri)
        val settings: MongoClientSettings = MongoClientSettings.builder()
          .codecRegistry(MongoClient.DEFAULT_CODEC_REGISTRY)
          .clusterSettings(ClusterSettings.builder().applyConnectionString(connectionString).build())
          .connectionPoolSettings(ConnectionPoolSettings.builder().applyConnectionString(connectionString).build())
          .serverSettings(ServerSettings.builder().build()).credentialList(connectionString.getCredentialList)
          .sslSettings(SslSettings.builder().applyConnectionString(connectionString).build())
          .socketSettings(SocketSettings.builder().applyConnectionString(connectionString).build())
          .streamFactoryFactory(NettyStreamFactoryFactory())
          .build()
        MongoClient(settings)
      case _ => MongoClient(uri)
    }
  }

  def collection: MongoCollection[Document] = {
    val databaseName = config.getString("services.guestbook-database-name")
    logger.debug(s"Using MongoDB database: $databaseName")

    val collectionName = config.getString("services.guestbook-collection-name")
    logger.debug(s"Using MongoDB collection: $collectionName")

    val db: MongoDatabase = client.getDatabase(databaseName)
    db.getCollection(collectionName)
  }

  /**
    * Signs the guestbook
    */
  def sign(firstName: String, lastName: String, comment: String) = {
    val document = Document("firstName" -> firstName, "lastName" -> lastName, "comment" -> comment)
    logger.debug(s"Creating document: $document")
    collection.insertOne(document).toFuture
  }
}

/**
  * Defines the JSON formatter for all our message types to support implicit marshalling/unmarshalling
  */
trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val guestbookRequestFormat = jsonFormat3(GuestbookRequest.apply)
  implicit val guestbookResponseFormat = jsonFormat1(GuestbookResponse.apply)
}

/**
  * Defines an asynchronous non-blocking service to process Guestbook requests
  */
trait GuestbookService extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config = ConfigFactory.load()
  val logger: LoggingAdapter

  val guestbookLogic = new GuestbookLogic()

  /**
    * Defines how HTTP requests and responses should be handled.
    */
  val routes = {
    logRequestResult(config.getString("services.name")) {
      pathPrefix(config.getString("services.context")) {
        (post & entity(as[GuestbookRequest])) { guestbookRequest =>
          onComplete(Future(guestbookLogic.sign(guestbookRequest.firstName, guestbookRequest.lastName, guestbookRequest.comment))) {
            case Success(value) =>
              logger.debug(s"Request completed successfully: $value")
              complete(OK, GuestbookResponse(config.getString("services.response-message")))
            case Failure(ex: IllegalArgumentException) => complete(BadRequest -> ex.getMessage)
            case Failure(ex) => complete(InternalServerError -> ex.getMessage)
          }
        } ~
          get {
            complete(OK, "Welcome to the Guestbook service, please POST your name and comment")
          }
      }
    }
  }
}

/**
  * Runs the Akka HTTP toolkit around the Service
  */
object GuestbookAkkaHttpMicroservice extends App with GuestbookService {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}




