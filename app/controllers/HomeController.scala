package controllers

import javax.inject._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import services.LatLongResolver
import services.LatLongResolver.Coordinates

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(latLongResolver: LatLongResolver) extends Controller {

  def getDiscreteSteps(latO: String, lngO: String, latD: String, lngD: String) = Action.async { implicit request =>

    Try {
      val origin = Coordinates(
        latO.toDouble,
        lngO.toDouble
      )
      val destination = Coordinates(
        latD.toDouble,
        lngD.toDouble
      )
      latLongResolver.getDiscreteSteps(origin, destination)
    } match {
      case Success(value) => value.map { x =>
        Ok(Json.toJson(x))
      }
      case Failure(exception) =>  Future.successful(BadRequest(s"$exception"))
    }

  }
}
