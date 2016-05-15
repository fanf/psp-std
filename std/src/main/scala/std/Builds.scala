package psp
package std

/** The organization of the implicit builders is a black art.
  *  It might be better to generate StdBuilds[0-15] and put an implicit in
  *  each one so the prioritization is as unambiguous as scala allows.
  *
  *  We adapt CanBuildFrom into our builder, since there are zillions of them lying
  *  around and it lets us build scala collections at the end of a view with no more code.
  *  Scala maps are built with Tuples even though Product2 should suffice; the types
  *  are written out as Tuple2[K, V] and not (K, V) to emphasize I'm using Tuple on purpose.
  *  The rest of the time one should write it K -> V.
  */
import api._, all._

final class ViewsAs[A, R](val f: R => View[A]) extends AnyVal {
  def viewAs(xs: R): IdView[A, R] = new IdView(f(xs))
}
final class HasViewsAs[A, R](repr: R)(implicit z: ViewsAs[A, R]) {
  def m: IdView[A, R]                      = z viewAs repr
}
final class Builds[-A, +R](val f: View[A] => R) {
  def contraMap[B](g: B => A): Builds[B, R] = builds(f compose (_ map g))
  def map[S](g: R => S): Builds[A, S]       = builds(g compose f)
  def build(xs: View[A]): R                 = f(xs)
  def build(xs: Each[A]): R                 = build(new IdView(xs))
}

trait JavaBuilders0 {
  protected def forJava[A, M[X] <: jCollection[X]](empty: M[A]): Builds[A, M[A]]                         = Builds(empty)(identity)(_ add _)
  protected def forJavaMap[K, V, M[X, Y] <: jAbstractMap[X, Y]](empty: M[K, V]): Builds[K -> V, M[K, V]] = Builds(empty)(identity)((r, kv) => kv app r.put)
  implicit def buildJavaSet[A]: Builds[A, jSet[A]]                                                       = forJava(new jHashSet[A])
}
trait JavaBuilders extends JavaBuilders0 {
  implicit def buildJavaList[A]: Builds[A, jList[A]]          = forJava(new jArrayList[A])
  implicit def buildJavaMap[K, V]: Builds[K -> V, jMap[K, V]] = forJavaMap(new jHashMap[K, V])
}
trait ScalaBuilders0 extends JavaBuilders {
  implicit def forScala[A, That](implicit z: CanBuild[A, That]): Builds[A, That] = Builds(z())(_.result)(_ += _)
}
trait ScalaBuilders extends ScalaBuilders0 {
  implicit def forScalaMap[K, V, That](implicit z: CanBuild[scala.Tuple2[K, V], That]): Builds[K -> V, That] =
    forScala contraMap (_ app pair)
}
trait JvmBuilders0 extends ScalaBuilders {
  implicit def buildJvmArray[A : CTag]: Builds[A, Array[A]] = Builds(Array.newBuilder[A])(_.result)(_ += _)
}
trait JvmBuilders extends JvmBuilders0 {
  implicit def buildJvmString: Builds[Char, String] = Builds(new StringBuilder)(_.toString)(_ append _)
}
trait PspBuilders0 extends JvmBuilders {
  implicit def buildPspSet[A : Eq]: Builds[A, ExSet[A]]            = Builds.scalaSet[A] map (xs => new Pset(xs))
  implicit def buildPspMap[K : Eq, V]: Builds[K -> V, ExMap[K, V]] = ???
}
trait PspBuilders1 extends PspBuilders0 {
  implicit def buildPspList[A]: Builds[A, Plist[A]] = builds(xs => ll.foldRight[A, Plist[A]](xs, nil(), _ :: _))
}
trait PspBuilders extends PspBuilders1 {
  implicit def buildPspVec[A]: Builds[A, Vec[A]] = Builds.scalaVector[A] map (xs => new Vec(xs))
}
trait Builders extends PspBuilders
object Builders extends Builders

trait Converters0 {
  implicit def convertJavaIterable[A, CC[X] <: jIterable[X]](xs: CC[A]): IdView[A, CC[A]]           = intoView(xs)
  implicit def convertJavaMap[K, V, CC[K, V] <: jMap[K, V]](xs: CC[K, V]): IdView[K -> V, CC[K, V]] = intoView(xs)
  implicit def convertMonoView[A, R](xs: R)(implicit z: ViewsAs[A, R]): IdView[A, R]                = intoView(xs)
}
trait Converters1 extends Converters0 {
  implicit def convertScala[A, CC[X] <: sCollection[X]](xs: CC[A]): IdView[A, CC[A]] = intoView(xs)
}
trait Converters2 extends Converters1 {
  implicit def convertJvmArray[A](xs: Array[A]): IdView[A, Array[A]]            = intoView(xs)
  implicit def convertPspEach[A, CC[X] <: Each[X]](xs: CC[A]): IdView[A, CC[A]] = intoView(xs)
}
trait ConvertersOf extends Converters2

object Builds {
  def apply[B, A, R](newBuilder: => B)(result: B => R)(add: (B, A) => Unit): Builds[A, R] =
    new Builds(xs => result(doto(newBuilder)(b => xs foreach (x => add(b, x)))))

  def javaList[A]: Builds[A, jList[A]]               = ?
  def javaMap[K, V]: Builds[K -> V, jMap[K, V]]      = ?
  def javaSet[A]: Builds[A, jSet[A]]                 = ?
  def jvmArray[A : CTag]: Builds[A, Array[A]]        = ?
  def jvmString: Builds[Char, String]                = ?
  def pspList[A]: Builds[A, Plist[A]]                = ?
  def pspMap[K : Eq, V]: Builds[K -> V, ExMap[K, V]] = ?
  def pspSet[A : Eq]: Builds[A, ExSet[A]]            = ?
  def pspVec[A]: Builds[A, Vec[A]]                   = ?
  def scalaList[A]: Builds[A, sciList[A]]            = ?
  def scalaMap[K, V]: Builds[K -> V, sciMap[K, V]]   = ?
  def scalaSet[A]: Builds[A, sciSet[A]]              = ?
  def scalaVector[A]: Builds[A, sciVector[A]]        = ?
}