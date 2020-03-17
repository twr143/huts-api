package io.twr143.services
import cats.implicits._
import cats.Functor
import cats.data.{Kleisli, OptionT}
import org.http4s.{HttpRoutes, Request, Response}
import ch.qos.logback.classic.Logger
import cats.effect._

/**
 * Created by Ilya Volynin on 03.12.2019 at 14:28.
 */
case class HelloInterceptor (implicit log: Logger) {

  def logRequest[B[_]]: Request[B] => Request[B] = {
    r => log.warn(s"req logged ${r.toString}"); r
  }

  def logRequestK[B[_] : Sync](): Kleisli[OptionT[B, *], Request[B], Request[B]] =
    Kleisli { r =>
      OptionT.liftF(Sync[B].pure {
        log.warn(s"reqK logged ${r.toString}");
        r
      })
    }

  def logResponse[B[_]]: Response[B] => Response[B] = {
    r => log.warn(s"resp logged ${r.toString}"); r
  }
  def logResponseK[B[_] : Sync]: Kleisli[OptionT[B, *], Response[B], Response[B]] =
    Kleisli { r =>
      OptionT.liftF(Sync[B].pure {
        log.warn(s"respK logged ${r.toString}");
        r
      })
    }

  def wrap[B[_] : Effect](service: HttpRoutes[B]): HttpRoutes[B] =
    Kleisli { req: Request[B] =>
//            service(logRequest(log)(req)).map(logResponse(log))
//      (logRequestK andThen service run) (req).map(logResponse(log))
      logRequestK andThen service andThen logResponseK apply req
    }
}
