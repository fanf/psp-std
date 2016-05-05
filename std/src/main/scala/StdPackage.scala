package psp
package std

import psp.api._
import scala.{ collection => sc }
import sc.{ mutable => scm, immutable => sci }

abstract class AllExplicit extends PspApi with PspCreators {
  final val Array     = scala.Array
  final val Failure   = scala.util.Failure
  final val Nil       = scala.collection.immutable.Nil
  final val None      = scala.None
  final val Option    = scala.Option
  final val SafeLong  = spire.math.SafeLong
  final val Some      = scala.Some
  final val Success   = scala.util.Success
  final val Try       = scala.util.Try
  final val sciList   = sci.List
  final val sciMap    = sci.Map
  final val sciSeq    = sci.Seq
  final val sciSet    = sci.Set
  final val sciVector = sci.Vector
  final val scmMap    = scm.Map

  // Type aliases I don't like enough to have in the API.
  type Bag[A]               = ExMap[A, Precise]
  type Bool                 = Boolean
  type CanBuild[-Elem, +To] = scala.collection.generic.CanBuildFrom[_, Elem, To]
  type VdexRange          = Consecutive[Vdex]
  // type IndexRange           = Consecutive[Index]
  type IntRange             = Consecutive[Int]
  type LongRange            = Consecutive[Long]
  type Renderer             = Show[Doc]
  type UnbuildsAs[+A, R]    = Unbuilds[R] { type Elem <: A }
  type View2D[+A]           = View[View[A]]

  // Helpers for inference when calling 'on' on contravariant type classes.
  def eqBy[A]    = new EqBy[A]
  def orderBy[A] = new OrderBy[A]
  def showBy[A]  = new ShowBy[A]
  def hashBy[A]  = new HashBy[A]

  def render[A](x: A)(implicit z: Show[A]): String = z show x
  // def lexicalOrder: Order[String]                  = Order.fromInt(_ compareTo _)
  def inheritShow[A] : Show[A]                     = Show.Inherited
  def inheritEq[A] : Hash[A]                       = Eq.Inherited
  def referenceEq[A <: AnyRef] : Hash[A]           = Eq.Reference
  def stringEq[A] : Hash[A]                        = Eq.ToString
  // def shownEq[A: Show] : Hash[A]                   = hashBy[A](x => render(x))(Eq.ToString)

  def inView[A](mf: Suspended[A]): View[A] = new LinearView(Each(mf))

  final val NoIndex       = Index.invalid
  final val NoFile: jFile = jFile("")
  final val NoPath: jPath = jPath("")
  final val NoUri: jUri   = jUri("")

  private def stdout                    = scala.Console.out
  // private def putOut(msg: Any): Unit = sideEffect(stdout print msg, stdout.flush())
  private def echoOut(msg: Any): Unit   = stdout println msg
  private def aops[A](x: A)             = new ops.AnyOps(x)

  // def print[A: Show](x: A): Unit   = putOut(render(x))
  def println[A: Show](x: A): Unit = echoOut(render(x))
  // def anyprintln(x: Any): Unit     = echoOut(aops(x).any_s)

  def applyIfNonEmpty[A](x: A)(f: A => A)(implicit e: Eq[A], z: Empty[A]): A =
    if (e.eqv(x, z.empty)) x else f(x)

  def ??? : Nothing = throw new scala.NotImplementedError

  def classOf[A: CTag](): Class[_ <: A]      = classTag[A].runtimeClass.asInstanceOf[Class[_ <: A]]
  def classTag[A: CTag] : CTag[A]            = ?[CTag[A]]
  def classFilter[A: CTag] : Partial[Any, A] = Partial(x => aops(x).isClass[A], x => aops(x).castTo[A])

  // def transitiveClosure[A: Eq](root: A)(expand: A => Foreach[A]): View[A] = inView { f =>
  //   def loop(in: View[A], seen: View[A]): Unit = new ops.IViewOps(in) filterNot (new ops.HasEq(seen) contains _) match {
  //     case Each() => ()
  //     case in     => in foreach f ; loop(in flatMap expand, new DirectView((new Conversions(seen) toVec) ++ (new Conversions(in) toVec)))
  //   }
  //   loop(view(root), view())
  // }

  // @inline def timed[A](elapsed: Long => Unit)(body: => A): A = {
  //   val start = nanoTime
  //   val result = body
  //   elapsed(nanoTime - start)
  //   result
  // }

  // def assert(assertion: => Boolean, msg: => Any): Unit =
  //   if (!assertion) runtimeException("" + msg)

  // def abortTrace(msg: String): Nothing           = aops(new RuntimeException(msg)) |> (ex => try throw ex finally ex.printStackTrace)
  def bufferMap[A, B: Empty](): scmMap[A, B]        = scmMap[A, B]() withDefaultValue emptyValue[B]
  def indexRange(start: Int, end: Int): VdexRange = Consecutive.until(start, end) map (x => Index(x))
  def lformat[A](n: Int): FormatFun                 = new FormatFun(cond(n == 0, "%s", new Pstring("%%-%ds") format n))
  def noNull[A](value: A, orElse: => A): A          = if (value == null) orElse else value
  // def nullAs[A] : A                                 = null.asInstanceOf[A]
  // def optMap[A, B](x: A)(f: A => B): Option[B]      = if (x == null) None else Some(f(x))
  // def option[A](p: Boolean, x: => A): Option[A]  = if (p) Some(x) else None
  def randomPosInt(max: Int): Int                   = scala.util.Random.nextInt(max + 1)
  // def utf8(xs: Array[Byte]): Utf8                = new Utf8(xs)

  def make[R](xs: R): RemakeHelper[R]  = new RemakeHelper[R](xs)
  def make0[R] : MakeHelper[R]         = new MakeHelper[R]
  def make1[CC[_]] : MakeHelper1[CC]   = new MakeHelper1[CC]
  // def make2[CC[_,_]] : MakeHelper2[CC] = new MakeHelper2[CC]

  // def array[A: CTag](xs: A*): Array[A]              = xs.toArray[A]
  def cond[A](p: Bool, thenp: => A, elsep: => A): A = if (p) thenp else elsep
  def list[A](xs: A*): Plist[A]                     = new Conversions(view(xs: _*)) toPlist
  // def rel[K: Eq, V](xs: (K->V)*): ExMap[K, V]       = new Paired(vec(xs: _*))(Splitter(_._1, _._2)) toExMap
  def set[A: Eq](xs: A*): ExSet[A]                  = new Conversions(view(xs: _*)) toExSet
  def vec[A](xs: A*): Vec[A]                        = Vec[A](xs: _*)
  def view[A](xs: A*): DirectView[A, Vec[A]]        = new DirectView[A, Vec[A]](vec(xs: _*))
  def zip[A, B](xs: (A->B)*): ZipView[A, B]         = Zip zip1 view(xs: _*)
}
