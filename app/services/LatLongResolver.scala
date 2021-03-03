package services

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LatLongResolver @Inject()(wsClient: WSClient) {
  import LatLongResolver._


  def getDiscreteSteps(origin: Coordinates, destination: Coordinates): Future[List[Coordinates]] = {
    for {
      routesResponse <- getDirections(origin, destination)
      steps <- disintegrateRouteResponse(routesResponse, origin)
    } yield steps
  }

  /*===========================Private Methods===============================*/

  private def getDirections(origin: Coordinates, destination: Coordinates): Future[RouteResponse] = {

    val url = "https://maps.googleapis.com/maps/api/directions/json?" +
      s"origin=${origin.lat},${origin.lng}" +
      s"&destination=${destination.lat},${destination.lng}" +
      "&key=AIzaSyAb8ohmBXqtK4y2_a5CFnFnfLGiOsuwjIo&unitSystem=METRIC"
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
    val aD = distance / (earthRadiusInKm*1000)
    val theta = bearing.toRadians
    val la1 = coordinates.lat.toRadians
    val lo1 = coordinates.lng.toRadians

    val la2 = Math.asin(Math.sin(la1)*Math.cos(aD) + Math.cos(la1)*Math.sin(aD)*Math.cos(theta))

    val lo2 = {
      lo1 + Math.atan2(Math.sin(theta) * Math.sin(aD) * Math.cos(la1), Math.cos(aD) - Math.sin(la1)*Math.sin(la2))
    }

    Coordinates(la2.toDegrees, lo2.toDegrees)
  }


  /*Calculates distance between two coordinates.*/
  private def getDistanceFromLatLonInMeters(coordinates1: Coordinates, coordinates2: Coordinates): Double = {
    val dLat = (coordinates2.lat - coordinates1.lat).toRadians
    val dLon = (coordinates2.lng - coordinates1.lng).toRadians
    val a =
      Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(coordinates1.lat.toRadians) * Math.cos(coordinates2.lat.toRadians) * Math.sin(dLon/2) * Math.sin(dLon/2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    earthRadiusInKm * c * 1000
  }

  private def disintegrateRouteResponse(response: RouteResponse, origin: Coordinates): Future[List[Coordinates]] = {

    Future.successful {
      response.routes.flatMap { route =>
        route.legs.foldLeft(List.empty[Coordinates]) { (currentRouteSteps, leg) =>
          val legSteps = leg.steps.foldLeft(List.empty[Coordinates]){ (currentLegSteps, step) =>
            val decodedPolyLineCoordinates = decodePolylinePoints(step.polyline.points)
            var res = {
              if(currentLegSteps.isEmpty) List(step.start_location)
              else List.empty[Coordinates]
            }
            var prevExploredCoordinates = step.start_location
            var prevRemainingDistance: Double  = 0
            for(index <- 1 until decodedPolyLineCoordinates.length by 1){
              val distance = getDistanceFromLatLonInMeters(prevExploredCoordinates, decodedPolyLineCoordinates(index))
              if(distance + prevRemainingDistance > 50){
                val adjustedDistance = 50 - prevRemainingDistance
                val bearing = getBearing(prevExploredCoordinates, decodedPolyLineCoordinates(index))
                var currRemainingDistance = distance + prevRemainingDistance - 50
                val newCoordinates = createCoordinate(prevExploredCoordinates, bearing, adjustedDistance)
                res = res :+ newCoordinates
                prevExploredCoordinates = newCoordinates
                while(currRemainingDistance >= 50){
                  val newCoordinates = createCoordinate(prevExploredCoordinates, bearing, 50)
                  res = res :+ newCoordinates
                  prevExploredCoordinates = newCoordinates
                  currRemainingDistance = currRemainingDistance - 50
                }

                prevExploredCoordinates = decodedPolyLineCoordinates(index)
                prevRemainingDistance = currRemainingDistance
              }
              else {
                prevRemainingDistance += distance
                prevExploredCoordinates = decodedPolyLineCoordinates(index)
              }
            }
            currentLegSteps ++ res
          }
          currentRouteSteps ++ legSteps
        }
      }
    }

  }


  /**
    * This method breaks the polyline encoded string to list of coordinates.
    * Please refer https://developers.google.com/maps/documentation/utilities/polylinealgorithm for details.
    * */
  private def decodePolylinePoints(encoded: String): List[Coordinates] = {
    var poly = List.empty[Coordinates]
    var index: Int = 0
    val len = encoded.length()
    var lat: Double = 0
    var lng: Double = 0

    while (index < len) {
      var b: Int = 0
      var shift = 0
      var result = 0
      do {
        b = encoded.charAt(index) - 63
        index = index + 1
        result |= (b & 0x1f) << shift
        shift += 5
      } while (b >= 0x20)
      var dlat: Double = {
        if((result & 1) != 0){
          ~(result >> 1)
        } else {
          result >> 1
        }
      }
      lat += dlat

      shift = 0
      result = 0
      do {
        b = encoded.charAt(index) - 63
        index=index+1
        result |= (b & 0x1f) << shift
        shift += 5
      } while (b >= 0x20)

      var dlng: Double = {
        if((result & 1) != 0){
          ~(result >> 1)
        } else {
          result >> 1
        }
      }
      lng += dlng

      val p: Coordinates = Coordinates(
        lat / 1E5,
        lng / 1E5
      )
      poly = poly :+ p

    }
    poly
  }
}

object LatLongResolver {

  final val earthRadiusInKm = 6378.14

  case class Distance(value: Long)
  object Distance {
    implicit val formats = Json.format[Distance]
  }

  case class Coordinates(lat: Double, lng: Double)
  object Coordinates {
    implicit val formats = Json.format[Coordinates]
  }

  case class PolyLine(points: String)

  object PolyLine {
    implicit val formats = Json.format[PolyLine]
  }
  case class Step(distance: Distance, start_location: Coordinates, end_location: Coordinates, polyline: PolyLine)
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
