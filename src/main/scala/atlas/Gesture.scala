package atlas

/** The viewer's pointer gestures as a pure state machine, the same for a mouse, a pen and fingers: one
  * pointer dragging rotates, two pointers pinching zoom by the ratio of their distances, and a second tap (or
  * click) within [[doubleTapMs]] and [[doubleTapPx]] of the first resets the view.
  */
object Gesture:

  val doubleTapMs: Double = 300
  val doubleTapPx: Double = 30

  /** The pointers down (by id, at their last position) and the last tap (time, x, y). */
  final case class State(
      pointers: Map[Int, (Double, Double)] = Map.empty,
      lastTap: Option[(Double, Double, Double)] = None
  )

  enum Action:
    case Rotate(dx: Double, dy: Double)
    case Zoom(factor: Double)
    case Reset
    case Nothing

  private def distance(a: (Double, Double), b: (Double, Double)): Double =
    math.hypot(a._1 - b._1, a._2 - b._2)

  /** A pointer goes down at (x, y) at time t (ms). */
  def down(s: State, id: Int, x: Double, y: Double, t: Double): (State, Action) =
    val pointers = s.pointers.updated(id, (x, y))
    if pointers.size > 1 then (State(pointers, None), Action.Nothing)
    else
      val doubleTap = s.lastTap.exists((t0, x0, y0) =>
        t - t0 <= doubleTapMs && distance((x, y), (x0, y0)) <= doubleTapPx
      )
      if doubleTap then (State(pointers, None), Action.Reset)
      else (State(pointers, Some((t, x, y))), Action.Nothing)

  /** A pointer moves to (x, y); a pointer not down (a mouse hovering) does nothing. */
  def move(s: State, id: Int, x: Double, y: Double): (State, Action) =
    s.pointers.get(id) match
      case None       => (s, Action.Nothing)
      case Some(prev) =>
        val pointers = s.pointers.updated(id, (x, y))
        val action   = s.pointers.size match
          case 1 => Action.Rotate(x - prev._1, y - prev._2)
          case 2 =>
            val other  = s.pointers.collectFirst { case (i, p) if i != id => p }.get
            val before = distance(prev, other)
            if before < 1e-6 then Action.Nothing else Action.Zoom(distance((x, y), other) / before)
          case _ => Action.Nothing
        (s.copy(pointers = pointers), action)

  /** A pointer goes up, or is cancelled. */
  def up(s: State, id: Int): State = s.copy(pointers = s.pointers - id)

  /** Whether a gesture is in progress (the view is being dragged or pinched). */
  def active(s: State): Boolean = s.pointers.nonEmpty
