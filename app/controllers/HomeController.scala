package controllers

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.scaladsl.{Flow, Keep, Sink}
import javax.inject._
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc._
import services.Kafka
import util.Constants

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, kafka: Kafka, configuration: Configuration) (implicit assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page with a content message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action.async { implicit request =>
    Future.successful(
    Ok(views.html.index(routes.HomeController.ws().webSocketURL())))
  }
  val config = configuration.getOptional[Configuration]("akka.kafka.consumer").getOrElse(Configuration.empty)


  def ws = WebSocket.acceptOrResult[Any, String] { _ =>
    kafka.source(Constants.kafkaTopic) match {
      case Failure(e) =>
        Future.successful(Left(InternalServerError("Could not connect to Kafka")))
      case Success(source) =>
        val flow = Flow.fromSinkAndSource(Sink.ignore, source.map(_.record.value))
        Future.successful(Right(flow))
    }
  }


}
