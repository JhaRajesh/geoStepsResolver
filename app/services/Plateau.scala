package services

import services.Movements.Coordinates

abstract class Plateau {
  var boundary: Coordinates

  def intializeBoundary(coordinates: Coordinates) = {
    boundary = coordinates
  }

  def isCoordinateInBoundary(coordinates: Coordinates): Boolean = {
    if(coordinates.x >= 0 &&  coordinates.y >= 0 && coordinates.x <= boundary.x && coordinates.y <= boundary.y) {
      true
    } else {
      false
    }
  }
}