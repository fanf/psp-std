package psp
package std

import all._

/** "Native" psp collections.
  */
sealed abstract class Plist[A] extends Each[A] {
  def ::(head: A): Pcons[A] = Pcons(head, this)
  @inline final def foreach(f: A => Unit): Unit = {
    @tailrec def loop(xs: Plist[A]): Unit = xs match {
      case Pcons(hd, tl) => f(hd); loop(tl)
      case _             =>
    }
    loop(this)
  }
}
final case class Pcons[A](head: A, tail: Plist[A]) extends Plist[A] {
  def size = Size.One.atLeast
}
final case object Pnil extends Plist[Nothing] {
  def size = _0
  def apply[A](): Plist[A] = cast(this)
}

object Pstream {
  def empty[A] : Pstream[A] = Psnil()
  def apply[A](hd: => A, tl: => Pstream[A]): Pscons[A] = new Pscons(hd, tl)
  def unapply[A](x: Pstream[A]): Opt[A -> Pstream[A]] = x match {
    case x: Pscons[A] => some(x.head -> x.tail)
    case _            => none()
  }
}
sealed abstract class Pstream[+A] extends Each[A] {
  @inline final def foreach(f: A => Unit): Unit = {
    @tailrec def loop(xs: Pstream[A]): Unit = xs match {
      case Pstream(hd, tl) => f(hd); loop(tl)
      case _               =>
    }
    loop(this)
  }
}
final class Pscons[A](h: => A, t: => Pstream[A]) extends Pstream[A] {
  def size = Size.One.atLeast
  lazy val head = h
  lazy val tail = t
}
final case object Psnil extends Pstream[Nothing] {
  def size = _0
  def apply[A](): Pstream[A] = cast(this)
}

sealed class PunapplySeq[A](it: () => scIterator[A]) extends scSeq[A] {
  def iterator        = it()
  def apply(idx: Int) = iterator drop idx next
  def length: Int     = if (iterator.hasNext) MaxInt else 0
}

sealed abstract class Consecutive[+A] extends Indexed[A] {
  type CC [X] <: Consecutive[X]
  def in: Interval
  def map[B](g: A => B): CC[B]
  def applyLong(x: Long): A
  def isAfter(n: Long): Bool

  def startLong: Long             = in.startLong
  def containsLong(n: Long): Bool = in contains n
}
object Consecutive {
  private val Empty = new Closed[Nothing](Interval.empty, indexOutOfBoundsException)

  def empty[A]: Closed[A]                                    = Empty
  def apply[A](in: Interval.Closed, f: Long => A): Closed[A] = new Closed(in, f)
  def apply[A](in: Interval.Open, f: Long => A): Open[A]     = new Open(in, f)

  def unapply[A](r: Consecutive[A]): Opt[A -> Opt[A]] = r match {
    case r: Closed[A] if r.size.isZero => none()
    case r: Closed[A]                  => some(r.head -> some(r.last))
    case r: Open[A]                    => some(r.head -> none())
  }

  final class Open[+A](val in: Interval.Open, f: Long => A) extends Consecutive[A] with Indexed[A] {
    type CC[X] = Open[X]

    def isAfter(n: Long): Bool      = false
    def applyLong(x: Long): A       = f(x)
    def size                        = Infinite
    def apply(vdex: Vdex): A        = f(in(vdex))
    def foreach(g: A => Unit): Unit = in foreach (f andThen g)
    def map[B](g: A => B): Open[B]  = in map (f andThen g)
  }
  final class Closed[+A](val in: Interval.Closed, f: Long => A) extends Consecutive[A] with Direct[A] {
    type CC[X] = Closed[X]

    def isAfter(n: Long)             = in.exclusiveEnd <= n
    def applyLong(x: Long): A        = f(x)
    def apply(vdex: Vdex): A         = f(in(vdex))
    def map[B](g: A => B): Closed[B] = in map (f andThen g)
    def size: Precise                = in.size

    def last: A                          = apply(size.lastIndex)
    def drop(n: Precise): Closed[A]      = Consecutive(in drop n, f)
    def dropRight(n: Precise): Closed[A] = Consecutive(in dropRight n, f)
    def take(n: Precise): Closed[A]      = Consecutive(in take n, f)
    def takeRight(n: Precise): Closed[A] = Consecutive(in takeRight n, f)

    def slice(start: Vdex, len: Precise): Closed[A] = Consecutive(in.slice(start, len), f)
    def slice(r: VdexRange): Closed[A]              = Consecutive(in.slice(r), f)
  }
}
