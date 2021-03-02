package services

import services.Directions.{Direction, East, North, South, West}
import services.Movements.{Coordinates, LeftMove, MovementType, RightMove, StepMove}
import services.Directions.NextDirection

abstract class Robot {
  var direction: Direction
  var coordinates: Coordinates


  def nextMove(movementType: MovementType) = {
    // implement movement
    // left/right -don't check
    // m - check boundary increase value

    movementType match {
      case LeftMove | RightMove => coordinates
      case StepMove => direction match {
        case North => coordinates.copy(y = coordinates.y + 1)
        case South => coordinates.copy(y = coordinates.y - 1)
        case East => coordinates.copy(x = coordinates.x + 1)
        case West => coordinates.copy(x = coordinates.x - 1)
      }
    }
  }


  def updateCoordinates(newCoordinates: Coordinates) = {
    coordinates = newCoordinates
  }

  def updateDirection(movementType: MovementType) = {
    movementType match {
      case LeftMove =>
        direction = direction.leftDirection
      case RightMove =>
        direction = direction.rightDirection
      case _: MovementType =>
    }
  }
}
