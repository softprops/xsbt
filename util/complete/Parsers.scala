/* sbt -- Simple Build Tool
 * Copyright 2011  Mark Harrah
 */
package sbt.complete

	import Parser._
	import java.io.File
	import java.net.URI
	import java.lang.Character.{getType, MATH_SYMBOL, OTHER_SYMBOL, DASH_PUNCTUATION, OTHER_PUNCTUATION, MODIFIER_SYMBOL, CURRENCY_SYMBOL}

// Some predefined parsers
trait Parsers
{
	lazy val any: Parser[Char] = charClass(_ => true, "any character")

	lazy val DigitSet = Set("0","1","2","3","4","5","6","7","8","9")
	lazy val Digit = charClass(_.isDigit, "digit") examples DigitSet
	lazy val Letter = charClass(_.isLetter, "letter")
	def IDStart = Letter
	lazy val IDChar = charClass(isIDChar, "ID character")
	lazy val ID = IDStart ~ IDChar.* map { case x ~ xs => (x +: xs).mkString }
	lazy val OpChar = charClass(isOpChar, "symbol")
	lazy val Op = OpChar.+.string
	lazy val OpOrID = ID | Op

	def opOrIDSpaced(s: String): Parser[Char] =
		if(DefaultParsers.matches(ID, s))
			OpChar | SpaceClass
		else if(DefaultParsers.matches(Op, s))
			IDChar | SpaceClass
		else
			any

	def isOpChar(c: Char) = !isDelimiter(c) && isOpType(getType(c))
	def isOpType(cat: Int) = cat match { case MATH_SYMBOL | OTHER_SYMBOL | DASH_PUNCTUATION | OTHER_PUNCTUATION | MODIFIER_SYMBOL | CURRENCY_SYMBOL => true; case _ => false }
	def isIDChar(c: Char) = c.isLetterOrDigit || c == '-' || c == '_'
	def isDelimiter(c: Char) = c match { case '`' | '\'' | '\"' | /*';' | */',' | '.' => true ; case _ => false }

	lazy val NotSpaceClass = charClass(!_.isWhitespace, "non-whitespace character")
	lazy val SpaceClass = charClass(_.isWhitespace, "whitespace character")
	lazy val NotSpace = NotSpaceClass.+.string
	lazy val Space = SpaceClass.+.examples(" ")
	lazy val OptSpace = SpaceClass.*.examples(" ")

	lazy val NotBackTickClass = charClass(_ != '`', "Non Backtick character") | ('\\' ~> '`')
	lazy val BackTickClass = charClass(_ == '`', "Backtick character")
	lazy val NotBackTick = NotBackTickClass.+.string
	lazy val BackTick = BackTickClass.examples("`")

	lazy val NotDoubleQuoteClass = charClass(_ != '"',  "Non double quote character") | ('\\'~> '"')
	lazy val DoubleQuoteClass = charClass(_ == '"', "Double quote character")
	lazy val NotDoubleQuote = NotDoubleQuoteClass.+.string
	lazy val DoubleQuote = DoubleQuoteClass.examples("\"")

	lazy val NotSingleQuoteClass = charClass(_ != '\'',  "Non single quote character") | ('\\'~> '\'')
	lazy val SingleQuoteClass = charClass(_ == '\'', "Single quote character")
	lazy val NotSingleQuote = NotSingleQuoteClass.+.string
	lazy val SingleQuote = SingleQuoteClass.examples("'")

	lazy val URIClass = URIChar.+.string !!! "Invalid URI"

	lazy val URIChar = charClass(alphanum) | chars("_-!.~'()*,;:$&+=?/[]@%#")
	def alphanum(c: Char) = ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9')

	// TODO: implement
	def fileParser(base: File): Parser[File] = token(mapOrFail(NotSpace)(s => new File(s.mkString)), "<file>")

	lazy val Port = token(IntBasic, "<port>")
	lazy val IntBasic = mapOrFail( '-'.? ~ Digit.+ )( Function.tupled(toInt) )
	lazy val NatBasic = mapOrFail( Digit.+ )( _.mkString.toInt )
	private[this] def toInt(neg: Option[Char], digits: Seq[Char]): Int =
		(neg.toSeq ++ digits).mkString.toInt
	lazy val Bool = ("true" ^^^ true) | ("false" ^^^ false)

	def repsep[T](rep: Parser[T], sep: Parser[_]): Parser[Seq[T]] =
		rep1sep(rep, sep) ?? Nil
	def rep1sep[T](rep: Parser[T], sep: Parser[_]): Parser[Seq[T]] =
		(rep ~ (sep ~> rep).*).map { case (x ~ xs) => x +: xs }

	def some[T](p: Parser[T]): Parser[Option[T]] = p map { v => Some(v) }
	def mapOrFail[S,T](p: Parser[S])(f: S => T): Parser[T] =
		p flatMap { s => try { success(f(s)) } catch { case e: Exception => failure(e.toString) } }

	/** String parser used throughout sbt 0.10.* and 0.11.0*/
	def spaceDelimited(display: String): Parser[Seq[String]] = (token(Space) ~> token(NotSpace, display)).* <~ SpaceClass.*
	/** @return a list of strings delimited by the parsed value of `isp` containing the parsed value of `notp` */
	def delimitedStrings(isp: Parser[Char], notp: Parser[String])(display: String = "<arg>"): Parser[Seq[String]] =
		repsep(token(isp) ~> token(notp, display) <~ isp, SpaceClass.*)
	/** @return a list of strings delimited by a backtick (`) character */
	def backTickDelimited: String => Parser[Seq[String]] = delimitedStrings(BackTick, NotBackTick)_
	/** @return a list of strings delimited by a double quote (") character */
	def doubleQuoteDelimited: String => Parser[Seq[String]] = delimitedStrings(DoubleQuote, NotDoubleQuote)_
	/** @return a list of strings delimited by a single quote (') character */
	def singleQuoteDelimited: String => Parser[Seq[String]] = delimitedStrings(SingleQuote, NotSingleQuote)_
	/** Strings are delimeted by the backtick (`) character by default */
	def defaultDelimited(display:String): Parser[Seq[String]] = doubleQuoteDelimited(display)

	def flag[T](p: Parser[T]): Parser[Boolean] = (p ^^^ true) ?? false

	def trimmed(p: Parser[String]) = p map { _.trim }
	def Uri(ex: Set[URI]) = mapOrFail(URIClass)( uri => new URI(uri)) examples(ex.map(_.toString))
}
object Parsers extends Parsers
object DefaultParsers extends Parsers with ParserMain
{
	def matches(p: Parser[_], s: String): Boolean =
		apply(p)(s).resultEmpty.isValid
	def validID(s: String): Boolean = matches(ID, s)
}
