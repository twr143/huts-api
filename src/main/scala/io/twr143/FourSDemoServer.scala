package io.twr143
import cats.effect.IO
import cats.effect._
import ch.qos.logback.classic.{Level, Logger}
import fs2.Stream
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.{BlazeBuilder, BlazeServerBuilder}
import scala.concurrent.ExecutionContext.Implicits.global
import org.http4s.server.Router
import org.slf4j.LoggerFactory
import io.twr143.repo.HutRepository
import io.twr143.services.{HelloInterceptor, HelloService, HutsService}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.implicits._
object FourSDemoServer extends IOApp with Http4sDsl[IO] {

  implicit lazy val root: Logger = LoggerFactory.getLogger(s"${this.getClass.getCanonicalName}".replace("$", "")).asInstanceOf[ch.qos.logback.classic.Logger]

  val hello = HelloService.service[IO]

  val aggregateHello = HelloInterceptor().wrap(hello)
                                
  def httpApp(hutRepo: HutRepository[IO]) =
    Router("/" -> HutsService.service(hutRepo), "/h" -> aggregateHello).orNotFound

  def run(args: List[String]) =
    BlazeServerBuilder[IO]
      .bindHttp(8082, "0.0.0.0")
      .withHttpApp(httpApp(HutRepository.empty[IO])).serve
      .compile.drain.as(ExitCode.Success)
}
