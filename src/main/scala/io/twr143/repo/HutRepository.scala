package io.twr143.repo

import java.util.UUID
import java.util.concurrent.Executors

import cats.effect._
import cats.implicits._
import ch.qos.logback.classic.Logger
import io.twr143.entities._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

final case class HutRepository[F[_]](private val huts: ListBuffer[HutWithId])
                                    (implicit e: Effect[F],
                                     cs: ContextShift[F],
                                     timer: Timer[F],
                                     root: Logger) {

  val blockingEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))


  def makeIdInDifferentTP = cs.evalOn(blockingEC)(makeId)

  def makeId: F[String] = e.delay {
    root.warn("current thread in mkId: {}", Thread.currentThread().getId)
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
      uuid <- makeIdInDifferentTP
      _ <- cs.shift
      _ <- e.delay {
        root.warn("current thread in addhut: {}", Thread.currentThread().getId)
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

  def empty[F[_]](implicit m: Effect[F], cs: ContextShift[F], timer: Timer[F], root: Logger): HutRepository[F] = HutRepository[F](ListBuffer())
}
