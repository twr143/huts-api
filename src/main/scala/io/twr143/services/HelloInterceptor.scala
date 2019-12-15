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

  def logRequest[B[_]](implicit log: Logger): Request[B] => Request[B] = {
    r => log.warn(s"req logged ${r.toString}"); r
  }

  def logResponse[B[_]](implicit log: Logger): Response[B] => Response[B] = {
    r => log.warn(s"resp logged ${r.toString}"); r
  }

  def apply[B[_]](service: HttpRoutes[B])(implicit log: Logger, e: Effect[B]): HttpRoutes[B] =
    Kleisli { req: Request[B] =>
      service(logRequest(log)(req)).map(logResponse(log))
    }
}
