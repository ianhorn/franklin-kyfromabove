package com.azavea.franklin

import java.time.Instant

import cats.implicits._
import geotrellis.server.stac.TemporalExtent
import io.circe.{Decoder, Encoder}

package object database extends CirceJsonbMeta with GeotrellisWktMeta {

  def stringToInstant: String => Either[Throwable, Instant] =
    (s: String) => Either.catchNonFatal(Instant.parse(s))

  def temporalExtentToString(te: TemporalExtent): String = {
    te.value match {
      case Some(start) :: Some(end) :: _ if start != end => s"${start.toString}/${end.toString}"
      case Some(start) :: Some(end) :: _ if start == end => s"${start.toString}"
      case Some(start) :: None :: _                      => s"${start.toString}/.."
      case None :: Some(end) :: _                        => s"../${end.toString}"
    }
  }

  def temporalExtentFromString(str: String): Either[String, TemporalExtent] = {
    str.split("/").toList match {
      case ".." :: endString :: _ =>
        val parsedEnd: Either[Throwable, Instant] = stringToInstant(endString)
        parsedEnd match {
          case Left(_)             => Left(s"Could not decode instant: $str")
          case Right(end: Instant) => Right(TemporalExtent(None, end))
        }
      case startString :: ".." :: _ =>
        val parsedStart: Either[Throwable, Instant] = stringToInstant(startString)
        parsedStart match {
          case Left(_)               => Left(s"Could not decode instant: $str")
          case Right(start: Instant) => Right(TemporalExtent(start, None))
        }
      case startString :: endString :: _ =>
        val parsedStart: Either[Throwable, Instant] = stringToInstant(startString)
        val parsedEnd: Either[Throwable, Instant]   = stringToInstant(endString)
        (parsedStart, parsedEnd).tupled match {
          case Left(_)                               => Left(s"Could not decode instant: $str")
          case Right((start: Instant, end: Instant)) => Right(TemporalExtent(start, end))
        }
      case _ =>
        Either.catchNonFatal(Instant.parse(str)) match {
          case Left(_)           => Left(s"Could not decode instant: $str")
          case Right(t: Instant) => Right(TemporalExtent(t, t))
        }
    }
  }

  implicit val encoderTemporalExtent: Encoder[TemporalExtent] =
    Encoder.encodeString.contramap[TemporalExtent] { extent =>
      temporalExtentToString(extent)
    }

  implicit val decoderTemporalExtent: Decoder[TemporalExtent] = Decoder.decodeString.emap { str =>
    temporalExtentFromString(str)
  }
}
