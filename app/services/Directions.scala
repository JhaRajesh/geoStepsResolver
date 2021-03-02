package services

object Directions {

  sealed abstract class Direction(val asString: String)

  case object North extends Direction("N")
  case object South extends Direction("S")
  case object East extends Direction("E")
  case object West extends Direction("W")

  // 0,1,2,3

  // (0-1 + 4)%4, (0+1 +4)%4

  implicit class NextDirection(direction: Direction) {
    def leftDirection = {
      direction match {
        case North => West
        case South => East
        case East => North
        case West => South
      }
    }

    def rightDirection = {
      direction match {
        case North => East
        case South => West
        case East => South
        case West => North
      }
    }
  }

}
