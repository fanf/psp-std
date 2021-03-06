package psp
package std

import scala.{ Tuple3, collection => sc }
import java.{ lang => jl }
import java.nio.{ file => jnf }
import java.util.AbstractMap.SimpleImmutableEntry
import StdShow._, exp._, all.showsToDoc

trait ApiTypes extends ExternalTypes {
  // Aliases for internal and external types.
  type ->[+A, +B]         = scala.Product2[A, B]        // A less overconstrained product type.
  type |[+A, +B]          = scala.Either[A, B]          // The missing sum type.
  type Bool               = scala.Boolean
  type GTOnce[+A]         = sc.GenTraversableOnce[A]    // This is the beautifully named type at the top of scala collections
  type Id[+X]             = X
  type Index              = Vindex[Vindex.Zero.type]
  type Nth                = Vindex[Vindex.One.type]
  type Opt[+A]            = scala.Option[A]             // Placeholder
  type Pair[+A, +B]       = A -> B
  type PairOf[+A]         = A -> A
  type Pair2D[+A, +B]     = Pair[A->B, A->B]
  type Ref[+A]            = AnyRef with A               // Promotes an A <: Any into an A <: AnyRef.
  type Triple[+A, +B, +C] = scala.Product3[A, B, C]
  type Vdex               = Vindex[_]
  type sCollection[+A]    = sc.GenTraversable[A]        // named analogously to jCollection.
  type HashEqOrder[-A]    = Hash[A] with Eq[A] with Order[A]
  type HashEq[-A]         = Hash[A] with Eq[A]

  // Function types.
  type ?=>[-A, +B]   = scala.PartialFunction[A, B] // ?=> associates to the left instead of the right.
  type BinOp[A]      = BinTo[A, A]                 // binary operation
  type BinTo[-A, +R] = (A, A) => R
  type Relation[-A]  = BinTo[A, Bool]
  type Suspended[+A] = ToUnit[ToUnit[A]]
  type ToBool[-A]    = A => Bool
  type ToInt[-A]     = A => Int
  type ToLong[-A]    = A => Long
  type ToSelf[A]     = A => A
  type ToString[-A]  = A => String
  type ToUnit[-A]    = A => Unit
  type <:<[-A, +B]   = A => B
}

abstract class ApiValues extends ApiTypes {
  final val MaxInt  = jl.Integer.MAX_VALUE
  final val MaxLong = jl.Long.MAX_VALUE
  final val MinInt  = jl.Integer.MIN_VALUE
  final val MinLong = jl.Long.MIN_VALUE
  final val EOL     = jl.System getProperty "line.separator"

  def ??? : Nothing                                 = throw new scala.NotImplementedError
  def abort(msg: Any): Nothing                      = throw new RuntimeException(any"$msg")
  def assert(assertion: => Bool, msg: => Any): Unit = if (!assertion) assertionError(msg)
  def assertionError(msg: Any): Nothing             = throw new AssertionError(any"$msg")
  def illegalArgumentException(msg: Any): Nothing   = throw new IllegalArgumentException(any"$msg")
  def indexOutOfBoundsException(msg: Any): Nothing  = throw new IndexOutOfBoundsException(any"$msg")
  def noSuchElementException(msg: Any): Nothing     = throw new NoSuchElementException(any"$msg")

  def ?[A](implicit value: A): A                              = value
  def _0[A](implicit z: ZeroOne[A]): A                        = z.zero
  def _1[A](implicit z: ZeroOne[A]): A                        = z.one
  def emptyValue[A](implicit z: Empty[A]): A                  = z.empty
  def max[A](l: A, r: A)(implicit z: Order[A]): A             = if (z.less(l, r)) r else l
  def min[A](l: A, r: A)(implicit z: Order[A]): A             = if (z.less(l, r)) l else r
  def zcond[A](p: Bool, thenp: => A)(implicit z: Empty[A]): A = cond(p, thenp, z.empty)

  def classFilter[A: CTag]: Any ?=> A       = Fun.partial(isInstance[A], cast[A])
  def classOf[A: CTag](): Class[_ <: A]     = cast(classTag[A].runtimeClass)
  def classTag[A: CTag]: CTag[A]            = ?[CTag[A]]
  def isInstance[A: CTag](x: Any): Bool     = classOf[A]() isAssignableFrom x.getClass
  def newArray[A: CTag](len: Int): Array[A] = new Array[A](len)

  def castRef[A](value: A): Ref[A]                       = cast(value)
  def cast[A](value: Any): A                             = value.asInstanceOf[A]
  def classNameOf(x: Any): String                        = JvmName asScala x.getClass short
  def cond[A](p: Bool, thenp: => A, elsep: => A): A      = if (p) thenp else elsep
  def doto[A](x: A)(f: A => Unit): A                     = doalso(x)(f(x))
  def doalso[A](x: A)(exprs: Unit*): A                   = x
  def fst[A, B](x: A -> B): A                            = x._1
  def identity[A](x: A): A                               = x
  def jFile(path: String): jFile                         = new jFile(path)
  def jPair[A, B](x: A, y: B): jMapEntry[A, B]           = new SimpleImmutableEntry(x, y)
  def jPath(path: String): jPath                         = jnf.Paths get path
  def jUri(x: String): jUri                              = java.net.URI create x
  def lformat[A](n: Int): A => String                    = stringFormat(cond(n <= 0, "%s", new Pstring("%%-%ds") format n), _)
  def none[A](): Option[A]                               = scala.None
  def nullAs[A]: A                                       = cast(null)
  def pair[A, B](x: A, y: B): Tuple2[A, B]               = Tuple2(x, y)
  def sideEffect[A](result: A, exprs: Any*): A           = result
  def snd[A, B](x: A -> B): B                            = x._2
  def some[A](x: A): Option[A]                           = scala.Some(x)
  def swap[A, B](x: A, y: B): B -> A                     = Tuple2(y, x)
  def triple[A, B, C](x: A, y: B, z: C): Tuple3[A, B, C] = new Tuple3(x, y, z)

  /** Safe in the senses that it won't silently truncate values,
    *  and will translate MaxLong to MaxInt instead of -1.
    *  Note that we depend on this.
    */
  def safeLongToInt(value: Long): Int = value match {
    case MaxLong => MaxInt
    case MinLong => MinInt
    case _       => assert(MinInt <= value && value <= MaxInt, pp"$value out of range"); value.toInt
  }

  def stringFormat(s: String, args: Any*): String = {
    def unwrapArg(arg: Any): AnyRef = arg match {
      case x: scala.math.ScalaNumber => x.underlying
      case x                         => castRef(x)
    }
    java.lang.String.format(s, args map unwrapArg: _*)
  }
}
