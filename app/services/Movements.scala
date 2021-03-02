package services

object Movements {

  case class Coordinates(x: Int, y: Int)


  sealed abstract class MovementType(asString: String)

  case object LeftMove extends MovementType("L")
  case object RightMove extends MovementType("R")
  case object StepMove extends MovementType("M")

}
