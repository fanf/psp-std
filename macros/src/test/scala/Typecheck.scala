package psp
package tests

import psp._, std._, all._, macros._, StdShow._
import Expressions._

class Typecheck extends ScalacheckBundle {
  def bundle = "Verifying Expected Failures"

  implicit def showTypechecked = Show[Typechecked](_.to_s)

  /** These are here so we do not accidentally rename them, making the shadowing below
   *  useless.
   */
  private val _ = {
    stringToPstring _
  }

  // We don't want to protect scala library from itself so let's unmask augmentString etc.
  def checkScala() = {
    // Shadowing the implicit.
    val stringToPstring = null
    identity(stringToPstring)
    // This import is actually used in the test below
    import scala.Predef._
    divide("scala-library", typecheckedLines(scalaLibraryCode), expectedTypecheck = 24)
  }

  /** We'll say a line which begins with the shown comment is expected to type check.
   *  Will make this more robust. For now this makes it easy to put the expectation of
   *  success or failure next to the code in question.
   */
  def divide(what: String, xs: sciVector[Typechecked]): NamedProp = divide(what, xs, xs count (_.code startsWith "/* ok */"))

  def divide(what: String, xs: sciVector[Typechecked], expectedTypecheck: Precise): NamedProp = {
    val good -> bad = xs.toVec partition (_.typechecks) mapBoth (_.force)

    def good_s = "good: " + (good.asShown joinWith "\n  ")
    def bad_s  = "bad: "  + (bad.asShown joinWith "\n  ")
    def label  = pp"$good_s\n\n$bad_s\n"

    NamedProp(
      pp"$expectedTypecheck/${xs.size} expressions from $what should typecheck",
      Prop(expectedTypecheck === good.size) :| label
    )
  }

  def props = vec[NamedProp](
    divide("psp-show", typecheckedLines(pspShowCode)),
    divide("psp-by-equals", typecheckedLines(pspByEquals), expectedTypecheck = 12),
    divide("psp-by-ref", typecheckedLines(pspByRef), expectedTypecheck = 0),
    divide("psp-straight", typecheckedLines(scalaLibraryCode), expectedTypecheck = 14),
    checkScala()
  )
}
