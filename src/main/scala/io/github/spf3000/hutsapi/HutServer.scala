package io.github.spf3000.hutsapi
import cats.effect.IO
import cats.Monad
import cats.FlatMap
import cats.implicits._
import cats.effect._
import fs2.Stream
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import org.http4s.dsl.io._
import org.http4s.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import entities.Hut
import entities._
import io.circe.Encoder
import org.http4s.server.Router
import scala.util.control.NonFatal

//import org.http4s.circe.CirceEntityEncoder._
object HutServer extends IOApp with Http4sDsl[IO] {

  val HUTS = "huts"

  implicit def jsonEncoder[A <: Product : Encoder, F[_] : Sync]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  def service[F[_]](hutRepo: HutRepository[F])(implicit F: Effect[F]) =
    HttpRoutes.of[F] {
      case GET -> Root / HUTS / hutId =>
        hutRepo.getHut(hutId)
          .map {
            case Some(hut) => Response(status = Status.Ok).withEntity(hut.asJson)
            case None => Response(status = Status.NotFound)
          }
      case GET -> Root / HUTS =>
        hutRepo.getAll.map(all => Response(status = Status.Ok).withEntity(all.asJson))
      case req@POST -> Root / HUTS =>
        req.decodeJson[Hut]
          .flatMap(hutRepo.addHut)
          .map { hut => Response(status = Status.Created).withEntity(hut.asJson) }
          .handleError { case NonFatal(e) =>
            Response(status = Status.BadRequest).withEntity(e.getMessage.asJson)
          }
      case req@PUT -> Root / HUTS =>
        req.decodeJson[HutWithId]
          .flatMap(hutRepo.updateHut)
          .flatMap(_ => F.pure(Response(status = Status.Ok)))
      case DELETE -> Root / HUTS / hutId =>
        hutRepo.deleteHut(hutId)
          .flatMap(_ => F.pure(Response(status = Status.NoContent)))
    }

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  implicit override val timer: Timer[IO] = IO.timer(global)

  def httpApp(hutRepo: HutRepository[IO]) = Router("/" -> service(hutRepo)).orNotFound

  def run(args: List[String]) =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(httpApp(HutRepository.empty[IO])).serve
      .compile.drain.as(ExitCode.Success)
}
