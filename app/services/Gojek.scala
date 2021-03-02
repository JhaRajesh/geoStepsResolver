package services

import java.util.Scanner

import services.Directions.{Direction, East, North, South, West}
import services.Movements.{Coordinates, LeftMove, RightMove, StepMove}

import scala.io.StdIn


object Gojek extends App {


  val coordinatesInput = StdIn.readLine()
  val coordinates = new Scanner(coordinatesInput)
  val coordinatesX = coordinates.nextInt()
  val coordinatesY = coordinates.nextInt()
  val plateauCoordinates = Coordinates(coordinatesX, coordinatesY)

  val botCoordinatesInput = StdIn.readLine()
  val botCoordinatesStr = new Scanner(botCoordinatesInput)

  val botCoordinates = Coordinates(botCoordinatesStr.nextInt(), botCoordinatesStr.nextInt())

  val botDirection = botCoordinatesStr.next() match {
    case "N" => North
    case "S" => South
    case "E" => East
    case "W" => West
  }

  val plateau = new Plateau {
    override var boundary: Coordinates = plateauCoordinates
  }

  val robot = new Robot {
    override var direction: Direction = botDirection
    override var coordinates: Coordinates = botCoordinates
  }




  val stepsMoves: String = StdIn.readLine()


  for (stepStr <- stepsMoves) {
    val step = stepStr match {
      case 'L' => LeftMove
      case 'R' => RightMove
      case 'M' => StepMove
    }

    val possibleCoordinates = robot.nextMove(step)

    if (plateau.isCoordinateInBoundary(possibleCoordinates)) {
      robot.updateCoordinates(possibleCoordinates)
    } else {
      throw new Exception(s"Robot out of boundary space")
    }
    robot.updateDirection(step)
  }


  print(robot.coordinates.x + " " + robot.coordinates.y + " ")
  println(robot.direction.asString)


}
