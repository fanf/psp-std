package psp
package std

import api._, all._

/** Motley objects for which a file of residence is not obvious.
  */
class FormatFun(val fmt: String) extends (Any => String) with ShowSelf {
  def apply(x: Any): String = stringFormat(fmt, x)
  def to_s                  = fmt
}
final class JvmName(val raw: String) extends ShowSelf {
  def segments: Vec[String] = raw splitChar '.'
  def short: String         = segments.last
  def to_s: String          = raw
}
object JvmName {
  import scala.reflect.NameTransformer.decode

  def asJava(clazz: jClass): JvmName  = new JvmName(clazz.getName)
  def asScala(clazz: jClass): JvmName = new JvmName(clazz.getName.mapSplit('.')(decode))
}
final class Utf8(val bytes: Array[Byte]) extends AnyVal with ShowSelf {
  def chars: Array[Char] = scala.io.Codec fromUTF8 bytes
  def to_s: String       = new String(chars)
}

trait StdEq0 {
  implicit def sizeEq: Hash[Size] = byEquals
  implicit def comparableOrder[A](implicit ev: A <:< Comparable[A]): Order[A] =
    Order(((x: A, y: A) => x compareTo y) andThen (x => longCmp(x)))
}
trait StdEq1 extends StdEq0 {
  implicit def eqViewsAs[R, A](implicit b: ViewsAs[A, R], z: Eq[A]): Eq[R] =
    Eq((xs, ys) => intoView(xs) zip intoView(ys) corresponds z.eqv)

  implicit def enumOrder[A](implicit ev: A <:< jEnum[_]): Order[A] =
    orderBy[A](_.ordinal)

  implicit def optionEq[A: Eq] : Eq[Opt[A]] = Eq {
    case (None, None)       => true
    case (Some(x), Some(y)) => x === y
    case _                  => false
  }

  implicit def longHashEqOrd: HashEqOrd[Long]       = HashEqOrd.Longs
  implicit def boolHashEqOrd: HashEqOrd[Bool]       = HashEqOrd by (x => if (x) 1 else 0)
  implicit def charHashEqOrd: HashEqOrd[Char]       = HashEqOrd by (x => x: Long)
  implicit def intHashEqOrd: HashEqOrd[Int]         = HashEqOrd by (x => x: Long)
  implicit def vindexHashEqOrd: HashEqOrd[Vdex]     = HashEqOrd by (_.indexValue)
  implicit def preciseHashEqOrd: HashEqOrd[Precise] = HashEqOrd by (_.getLong)
  implicit def stringHashEqOrd: HashEqOrd[String]   = HashEqOrd.inherited[String]

  implicit def classEq: Hash[Class[_]] = byEquals

  implicit def tryEq[A](implicit z1: Eq[A], z2: Eq[Throwable]): Eq[Try[A]] = Eq {
    case (Success(x), Success(y)) => x === y
    case (Failure(x), Failure(y)) => x === y
    case _                        => false
  }
}
trait StdEq extends StdEq1 {
  implicit def product2HashEqOrder[A: HashEqOrd, B: HashEqOrd]: HashEqOrd[A -> B] =
    HashEqOrd[A -> B](
      (x, y) => fst(x) === fst(y) && snd(x) === snd(y),
      orderBy[A -> B](fst) | snd cmp,
      xy => fst(xy).hash + snd(xy).hash
    )
}

object StdShow extends ShowInstances
object Unsafe {
  implicit def promoteIndex(x: Long): Index = Index(x)
  implicit def inheritedShow[A]: Show[A]    = inheritShow
}

object +: {
  def unapply[A](xs: View[A]): Option[A -> View[A]] =
    cond(xs.isEmpty, none, some(xs.head -> xs.tail))
}
object :+ {
  def unapply[A](xs: View[A]): Option[View[A] -> A] =
    cond(xs.isEmpty, none, some(xs.init -> xs.last))
}

sealed abstract class <:<[-From, +To] extends (From => To)
final class conformance[A]            extends <:<[A, A] { def apply(x: A): A = x }

final class LabeledFunction[-T, +R](f: T => R, val to_s: String) extends (T ?=> R) {
  def isDefinedAt(x: T) = f match {
    case f: scala.PartialFunction[_, _] => f isDefinedAt x
    case _                              => true
  }
  def apply(x: T): R    = f(x)
  override def toString = to_s
}