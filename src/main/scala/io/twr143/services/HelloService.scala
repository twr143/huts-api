package io.twr143.services
import cats.effect._
import ch.qos.logback.classic.Logger
import io.circe.Encoder
import org.http4s.{HttpRoutes, Response, Status}
import io.circe.syntax._
import org.http4s.dsl.io.{->, /, GET, Root}
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
//import org.http4s.dsl.io._
//import org.http4s.implicits._
//import cats.Monad
//import cats.FlatMap
//import cats.implicits._
/**
  * Created by Ilya Volynin on 03.12.2019 at 12:07.
  */
object HelloService {

  val HALO = "halo"

  implicit def jsonEncoder[A <: Product : Encoder, F[_] : Sync]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  def service[F[_]:Effect](implicit logger: Logger) =
    HttpRoutes.of[F] {
      case GET -> Root / HALO / name =>
        Effect[F].pure(Response(status = Status.Ok).withEntity(s"halo, $name".asJson))
    }
}
