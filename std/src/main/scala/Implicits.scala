package psp
package std

import scala.{ collection => sc }

trait PackageImplicits extends ImplicitRemoval with StandardImplicits4 with ArrowAssoc2 with ShowImplicits2

/** Various lame global-scope implicits, made to disappear with our friend null.
 *  This list is subject to renegotiation.
 */
trait ImplicitRemoval {
  val any2stringadd, fallbackStringCanBuildFrom                                                           = null
  val tuple2ToZippedOps, tuple3ToZippedOps                                                                = null
  val wrapString, unwrapString, augmentString, unaugmentString                                            = null
  val StringAdd, ArrowAssoc, Boolean2boolean, Byte2byte, Character2char                                   = null
  val Double2double, Float2float, Integer2int, Long2long, Short2short                                     = null
  val genericWrapArray, wrapBooleanArray, wrapByteArray, wrapCharArray, wrapDoubleArray, wrapFloatArray   = null
  val wrapIntArray, wrapLongArray, wrapRefArray, wrapShortArray, wrapUnitArray                            = null
  val byteWrapper, shortWrapper, charWrapper, intWrapper, longWrapper, floatWrapper, doubleWrapper        = null
  val byteArrayOps, shortArrayOps, charArrayOps, intArrayOps, longArrayOps, floatArrayOps, doubleArrayOps = null
  val genericArrayOps                                                                                     = null
}

trait StandardImplicits1 {
  implicit def arrayBuilder[A: ClassTag] : Builds[A, Array[A]] = Builds(xs =>
    xs.sizeInfo match {
      case Precise(Size(n)) =>
        val result = new Array[A](n)
        var i = 0
        xs foreach (x => try result(i) = x finally i += 1)
        result
      case _ => Array.newBuilder[A] ++= xs.trav result
    }
  )

  // aka AtomicView[Repr, tc.type] but SI-8223. Similarly for the analogous implicits.
  implicit def raiseAtomicView[Repr](repr: Repr)(implicit tc: Foreachable[Repr]): Env[Repr, tc.type]#AtomicView = tc wrap repr
}
trait StandardImplicits2 extends StandardImplicits1 {
  implicit def raiseIndexedView[Repr](repr: Repr)(implicit tc: DirectAccess[Repr]): Env[Repr, tc.type]#IndexedView = tc wrap repr
}

trait StandardImplicits3 extends StandardImplicits2 {
  // A weaker variation of Shown - use Show[A] if one can be found and toString otherwise.
  implicit def showableToTryShown[A](x: A)(implicit shows: Show[A] = Show.native[A]): TryShown = new TryShown(shows show x)
  // Deprioritize PartialOrder vs. Order since they both have comparison methods.
  implicit def tclassPartialOrderOps[A: PartialOrder](x: A): TClass.PartialOrderOps[A] = new TClass.PartialOrderOps[A](x)
  // Implicit conversions between type classes.
  implicit def orderToOrdering[A](implicit ord: Order[A]): Ordering[A] = Ordering fromLessThan ((x, y) => ord.compare(x, y).intValue < 0)
}

trait StandardImplicits4 extends StandardImplicits3 {
  // The typesafe non-toString-using show"..." interpolator.
  implicit def showStringContextOps(sc: StringContext): ShowInterpolator = new ShowInterpolator(sc)
  // Continuing the delicate dance against scala's hostile-to-correctness intrinsics.
  implicit def showableToShown[A: Show](x: A): Shown = new Shown(show[A] show x)
  implicit def showLabeled = Show[api.Labeled](_.label)

  // We buried Predef's {un,}augmentString in favor of these.
  @inline final implicit def pspAugmentString(x: String): PspStringOps   = new PspStringOps(x)
  @inline final implicit def pspUnaugmentString(x: PspStringOps): String = x.toString

  // Typeclass requiring extension methods. There is something very depraved going on here,
  // see if you can tell what it is.
  implicit def tclassShowDirectOps[A: Show](x: A): TClass.ShowDirectOps        = new TClass.ShowDirectOps(x.to_s)
  implicit def tclassOrderOps[A: Order](x: A): TClass.OrderOps[A]              = new TClass.OrderOps[A](x)
  implicit def tclassAlgebraOps[A: BooleanAlgebra](x: A): TClass.AlgebraOps[A] = new TClass.AlgebraOps[A](x)
  implicit def tclassEqOps[A: Eq](x: A): TClass.EqOps[A]                       = new TClass.EqOps[A](x)
  implicit def tclassHasForeach[R: Has.Foreach](xs: R)                         = opsForeach(Foreach(?[Has.Foreach[R]] hasForeach xs))

  // Direct-acting extension methods. These are extension methods installed directly onto the
  // type of interest, as opposed to involving a typeclass.
  implicit def opsAny[A](x: A): Ops.AnyOps[A]                                         = new Ops.AnyOps[A](x)
  implicit def opsArray[A](xs: Array[A]): Ops.ArrayOps[A]                             = new Ops.ArrayOps[A](xs)
  implicit def opsBooleanAlgebra[A](alg: BooleanAlgebra[A]): Ops.BooleanAlgebraOps[A] = new Ops.BooleanAlgebraOps[A](alg)
  implicit def opsEq[A](x: Eq[A]): Ops.EqOps[A]                                       = new Ops.EqOps[A](x)
  implicit def opsForeach[A](xs: Foreach[A]): Ops.ForeachOps[A]                       = new Ops.ForeachOps(xs)
  implicit def opsFunction1[T, R](f: T => R): Ops.Function1Ops[T, R]                  = new Ops.Function1Ops[T, R](f)
  implicit def opsGTOnce[CC[X] <: GTOnce[X], A](xs: CC[A]): Ops.GTOnce[CC, A]         = new Ops.GTOnce[CC, A](xs)
  implicit def opsInputStream(x: InputStream): Ops.InputStreamOps                     = new Ops.InputStreamOps(x)
  implicit def opsInt(x: Int): Ops.IntOps                                             = new Ops.IntOps(x)
  implicit def opsIterator[A](it: jIterator[A]): Ops.IteratorOps[A]                   = new Ops.IteratorOps(it)
  implicit def opsCollection[A](x: jAbstractCollection[A]): Ops.jCollectionOps[A]     = new Ops.jCollectionOps(x)
  implicit def opsLong(x: Long): Ops.LongOps                                          = new Ops.LongOps(x)
  implicit def opsMap[K, V](xs: sc.Map[K, V]): Ops.Map[K, V]                          = new Ops.Map[K, V](xs)
  implicit def opsOption[A](x: Option[A]): Ops.OptionOps[A]                           = new Ops.OptionOps[A](x)
  implicit def opsOrder[A](x: Order[A]): Ops.OrderOps[A]                              = new Ops.OrderOps[A](x)
  implicit def opsSeq1[CC[X] <: sc.Seq[X], A](xs: CC[A]): Ops.Seq1[CC, A]             = new Ops.Seq1[CC, A](xs)
  implicit def opsSeq2[CC[X] <: sc.Seq[X], A](xs: CC[A]): Ops.Seq2[CC, A]             = new Ops.Seq2[CC, A](xs)
  implicit def opsSeqOps[CC[X] <: sc.Seq[X], A](xs: CC[A]): Ops.SeqOps[CC, A]         = new Ops.SeqOps[CC, A](xs)
  implicit def opsShow[A](x: Show[A]): Ops.ShowOps[A]                                 = new Ops.ShowOps[A](x)
  implicit def opsSortedMap[K, V](xs: sc.SortedMap[K, V]): Ops.SortedMap[K, V]        = new Ops.SortedMap[K, V](xs)
  implicit def opsTry[A](x: scala.util.Try[A]): Ops.TryOps[A]                         = new Ops.TryOps[A](x)
}

trait ShowImplicits1 {
  implicit def intShow     = Show.native[Int]()
  implicit def longShow    = Show.native[Long]()
  implicit def doubleShow  = Show.native[Double]()
  implicit def booleanShow = Show.native[Boolean]()
  implicit def charShow    = Show.native[Char]()
}
trait ShowImplicits2 extends ShowImplicits1 {
  def inBrackets[A: Show](xs: A*): String = Seq("[", xs.toSeq.joinComma, "]") join ""

  /** An incomplete selection of show compositors.
   *  Not printing the way scala does.
   */
  implicit def stringShow: Show[String]                        = Show(x => x)
  implicit def optShow[A: Show] : Show[Option[A]]              = Show(_.fold("-")(x => show"$x"))
  implicit def seqShow[CC[X] <: Seq[X], A: Show] : Show[CC[A]] = Show(xs => inBrackets(xs: _*))
  implicit def arrayShow[A: Show] : Show[Array[A]]             = Show(xs => inBrackets(xs: _*))
  implicit def tupleShow[A: Show, B: Show] : Show[(A, B)]      = Show { case (x, y) => show"$x -> $y" }
  implicit def showDirect[A <: ShowDirect] : Show[A]           = Show.native[A]()
  implicit def numberShow[A <: ScalaNumber] : Show[A]          = Show.native[A]() // BigInt, BigDecimal
}
trait ReadImplicits {
  implicit val bigIntRead = Read[BigInt](s => BigInt(s))
  implicit val bigDecRead = Read[BigDecimal](s => BigDecimal(s))
  implicit val stringRead = Read[String](s => s)
  implicit val floatRead  = Read[Float](_.toFloat)
  implicit val doubleRead = Read[Double](_.toDouble)
  implicit val longRead   = Read[Long](_.toLong)
  implicit val intRead    = Read[Int](_.toInt)
}

trait ArrowAssoc1 {
  @inline final implicit def arrowAssocRef[A](x: A): Ops.ArrowAssocRef[A] = new Ops.ArrowAssocRef(x)
}
trait ArrowAssoc2 extends ArrowAssoc1 {
  @inline final implicit def arrowAssocInt(x: Int): Ops.ArrowAssocInt             = new Ops.ArrowAssocInt(x)
  @inline final implicit def arrowAssocLong(x: Long): Ops.ArrowAssocLong          = new Ops.ArrowAssocLong(x)
  @inline final implicit def arrowAssocDouble(x: Double): Ops.ArrowAssocDouble    = new Ops.ArrowAssocDouble(x)
  @inline final implicit def arrowAssocChar(x: Char): Ops.ArrowAssocChar          = new Ops.ArrowAssocChar(x)
  @inline final implicit def arrowAssocBoolean(x: Boolean): Ops.ArrowAssocBoolean = new Ops.ArrowAssocBoolean(x)
}