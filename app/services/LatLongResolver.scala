package services

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.libs.ws.WSClient
import services.LatLongResolver.Coordinates

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LatLongResolver @Inject()(wsClient: WSClient) {
  import LatLongResolver._
  private def getDirections(origin: Coordinates, destination: Coordinates): Future[RouteResponse] = {

    val url = "https://maps.googleapis.com/maps/api/directions/json?" +
      s"origin=${origin.lat},${origin.lng}" +
      s"&destination=${destination.lat},${destination.lng}" +
      "&key=AIzaSyAb8ohmBXqtK4y2_a5CFnFnfLGiOsuwjIo&unitSystem=METRIC&mode=walking"
    wsClient.url(url).get().map {
      case wsResponse if wsResponse.status == 200 =>
        Json.parse(wsResponse.body).validate[RouteResponse](RouteResponse.formats) match {
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw new Exception(s"Error while parsing Route response dto. Please check the directions api for details: $errors")
        }
      case other => throw new Exception(s"Invalid status: ${other.status}")
    }
  }

  private def getBearing(start: Coordinates, end: Coordinates) = {
    val startLat = start.lat.toRadians
    val startLng = start.lng.toRadians
    val endLat = end.lat.toRadians
    val endLng = end.lng.toRadians
    val dLong = endLng - startLng

    val x = Math.sin(dLong)*Math.cos(endLat)

    /*
    X = cos(b.lat)* sin(dL)
    Y = cos(a.lat)*sin(b.lat) - sin(a.lat)*cos(b.lat)* cos(dL)
    */
    val y = Math.cos(startLat)*Math.sin(endLat) - Math.sin(startLat)*Math.cos(endLat)*Math.cos(dLong)
    Math.toDegrees(Math.atan2(x, y))
  }

  private def createCoordinate(coordinates: Coordinates, bearing: Double, distance: Double) = {
    //lat2 = asin(sin la1 * cos Ad  + cos la1 * sin Ad * cos θ)
    //lo2 = lo1 + atan2(sin θ * sin Ad * cos la1 , cos Ad – sin la1 * sin la2)
    val radius = 6378.14
    val aD = distance / (radius*1000)
    val theta = bearing.toRadians
    val la1 = coordinates.lat.toRadians
    val lo1 = coordinates.lng.toRadians

    val la2 = Math.asin(Math.sin(la1)*Math.cos(aD) + Math.cos(la1)*Math.sin(aD)*Math.cos(theta))

    val lo2 = {
      lo1 + Math.atan2(Math.sin(theta) * Math.sin(aD) * Math.cos(la1), Math.cos(aD) - Math.sin(la1)*Math.sin(la2))
    }

    Coordinates(la2.toDegrees, lo2.toDegrees)
  }

  private def disintegrateRouteResponse(response: RouteResponse): Future[List[Coordinates]] = {

    Future.successful {
      response.routes.flatMap { route =>
        route.legs.flatMap { leg =>
          leg.steps.flatMap { step =>
            val bearing = getBearing(step.start_location, step.end_location)
            var res = List(step.start_location)
            for (x <- 50L to step.distance.value by 50L) {
              val createCoordinates = createCoordinate(step.start_location, bearing, x)
              res = res :+ createCoordinates
            }
            res
          }
        }
      }
    }

  }

  def getSteps(origin: Coordinates, destination: Coordinates) = {
    for {
      routesResponse <- getDirections(origin, destination)
      steps <- disintegrateRouteResponse(routesResponse)
    } yield {
      steps :+ destination
    }
  }
}

object LatLongResolver {
  case class Distance(value: Long)
  object Distance {
    implicit val formats = Json.format[Distance]
  }

  case class Coordinates(lat: Double, lng: Double)
  object Coordinates {
    implicit val formats = Json.format[Coordinates]
  }
  case class Step(distance: Distance, start_location: Coordinates, end_location: Coordinates)
  object Step {
    implicit val formats = Json.format[Step]
  }

  case class Leg(distance: Distance, steps: List[Step])
  object Leg {
    implicit val formats = Json.format[Leg]
  }

  case class Routes(legs: List[Leg])
  object Routes {
    implicit val formats = Json.format[Routes]
  }
  case class RouteResponse(routes: List[Routes])
  object RouteResponse {
    implicit val formats = Json.format[RouteResponse]
  }

}
