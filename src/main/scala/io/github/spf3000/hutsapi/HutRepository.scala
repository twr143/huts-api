package io.github.spf3000.hutsapi
import java.util.UUID
import cats.effect._
import scala.collection.mutable.ListBuffer
import cats.FlatMap
import cats.implicits._
import cats.effect.IO
import io.github.spf3000.hutsapi.entities._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

final case class HutRepository[F[_]](private val huts: ListBuffer[HutWithId])
                                    (implicit e: Effect[F], cs: ContextShift[F], timer: Timer[F]) {

  val blockingEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  val makeId: F[String] = e.delay {
    UUID.randomUUID().toString
  }

  def getAll: F[List[HutWithId]] =
    e.delay {
      huts.toList
    }

  def getHut(id: String): F[Option[HutWithId]] =
    e.delay {
      huts.find(_.id == id)
    }

  def addHut(hut: Hut): F[String] =
    for {
      uuid <- makeId
      _ <- e.delay {
        huts += hutWithId(hut, uuid)
      }
    } yield uuid

  def updateHut(hutWithId: HutWithId): F[Unit] = {
    for {
      _ <- e.delay {
        huts.find(_.id == hutWithId.id).foreach(h => huts -= h)
      }
      _ <- e.delay {
        huts += hutWithId
      }
    } yield ()
  }

  def deleteHut(hutId: String): F[Unit] =
    e.delay {
      huts.find(_.id == hutId).foreach(h => huts -= h)
    }

  def hutWithId(hut: Hut, id: String): HutWithId =
    HutWithId(id, hut.name)
}

object HutRepository {

  def empty[F[_]](implicit m: Effect[F], cs: ContextShift[F], timer: Timer[F]): HutRepository[F] = new HutRepository[F](ListBuffer())
}
