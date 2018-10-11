package controllers

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import javax.inject._
import play.api.libs.json.JsValue
import play.api.mvc._
import services.Kafka
import util.Constants

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem, kafka: Kafka) (implicit assetsFinder: AssetsFinder)
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


  def ws = WebSocket.acceptOrResult[Any, String] { _ =>
    kafka.source(Constants.kafkaTopic) match {
      case Failure(e) =>
        Future.successful(Left(InternalServerError("Could not connect to Kafka")))
      case Success(source) =>
        val flow = Flow.fromSinkAndSource(Sink.ignore, source.map(_.value))
        Future.successful(Right(flow))
    }
  }

}
