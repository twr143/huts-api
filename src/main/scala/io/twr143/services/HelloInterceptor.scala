package io.twr143.services
import cats.Functor
import cats.data.Kleisli
import org.http4s.{HttpRoutes, Request, Response}
import ch.qos.logback.classic.Logger
import cats.effect._

/**
  * Created by Ilya Volynin on 03.12.2019 at 14:28.
  */
object HelloInterceptor {

  def logRequest[F[_]](implicit log: Logger): Request[F] => Request[F] = {
    r => log.warn(s"req logged ${r.toString}"); r
  }

  def logResponse[F[_]](implicit log: Logger): Response[F] => Response[F] = {
    r => log.warn(s"resp logged ${r.toString}"); r
  }

  def apply[F[_]](service: HttpRoutes[F])(implicit log: Logger, m: Effect[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      service(logRequest(log)(req)).map(logResponse(log))
    }
}
