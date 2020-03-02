package io.twr143.services
import cats.effect.{Effect, Sync}
import io.twr143.FourSDemoServer.{->, /, DELETE, GET, POST, PUT, Root}
import io.twr143.entities.{Hut, HutId, HutWithId}
import io.twr143.repo.HutRepository
import org.http4s.{EntityEncoder, HttpRoutes, Response, Status}
import org.http4s.circe.jsonEncoderOf
import io.circe.Encoder
import scala.util.control.NonFatal
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe._
import cats.implicits._

/**
 * Created by Ilya Volynin on 03.12.2019 at 10:42.
 */
object HutsService {

  val HUTS = "huts"

  implicit def jsonEncoder[A <: Product : Encoder, F[_] : Sync]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  def hutIdRoute[F[_]](hutRepo: HutRepository[F])(implicit F: Effect[F]) = HttpRoutes.of[F] {
    case GET -> Root / HUTS / hutId =>
      hutRepo.getHut(hutId)
        .map {
          case Some(hut) => Response(status = Status.Ok).withEntity(hut.asJson)
          case None => Response(status = Status.NotFound)
        }
  }

  def AllHutsRoute[F[_]](hutRepo: HutRepository[F])(implicit F: Effect[F]) = HttpRoutes.of[F] {
    case GET -> Root / HUTS =>
      hutRepo.getAll.map(all => Response(status = Status.Ok).withEntity(all.asJson))
  }

  def TheRestRoutes[F[_]](hutRepo: HutRepository[F])(implicit F: Effect[F]) = HttpRoutes.of[F] {
    case req@POST -> Root / "samples" =>
      req.decodeJson[HutId]
        .flatMap(hid => hutRepo.samples(hid.id))
        .map(all => //Response(status = Status.BadRequest)
          Response(status = Status.Ok).withEntity(all.asJson)
        )
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

  def service[F[_]](hutRepo: HutRepository[F])(implicit F: Effect[F]) =
    hutIdRoute(hutRepo) <+> AllHutsRoute(hutRepo) <+> TheRestRoutes(hutRepo)
}
