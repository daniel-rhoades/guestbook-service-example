package com.danielrhoades.examples.microservices.guestbook

import akka.event.NoLogging
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.Await
import org.mongodb.scala.model.Filters

class GuestbookSpec extends FlatSpec with Matchers with ScalatestRouteTest with GuestbookService {
  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig
  override val logger = NoLogging

  val bobSmithComment = GuestbookResponse("Your comment has been recorded")

  implicit val timeout: Timeout = 5.seconds

  "Service" should "respond to a valid firstName/lastName/comment submission" in {
    Post(s"/guestbook", GuestbookRequest("Bob", "Smith", "Oh what a wonderful place")) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[GuestbookResponse] shouldBe bobSmithComment

      val result = Await.ready(guestbookLogic.collection.find(Filters.equal("firstName", "Bob")).first().toFuture, 5.seconds).value.get.get
      result should have length 1

      Await.ready(guestbookLogic.collection.drop.toFuture, 10.seconds)
    }
  }
}
