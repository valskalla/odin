package io.odin.examples

import cats.data.WriterT
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import io.odin.{Logger, LoggerMessage, OdinSpec}
import io.odin.loggers.WriterTLogger

/**
  * This spec shows an example of how to test the logger using WriterT monad that is just an abstraction over `F[(Log, A)]`
  */
class SimpleAppSpec extends OdinSpec {

  //definition of test monad. It keeps all the incoming `LoggerMessage` inside of the list
  type WT[A] = WriterT[IO, List[LoggerMessage], A]
  implicit val ioRuntime: IORuntime = IORuntime.global

  "HelloSimpleService" should "log greeting call" in {
    //logger that writes messages as a log of WriterT monad
    val logger: Logger[WT] = new WriterTLogger[IO]
    val simpleService: HelloSimpleService[WT] = new HelloSimpleService(logger)

    val name = "UserName"

    //.written is the method of WriterT monad that returns IO[List[LoggerMessage]]
    val loggedMessage :: Nil = simpleService.greet(name).written.unsafeRunSync()

    //LoggerMessage.message contains the lazy evaluated log from simpleService.greet method
    loggedMessage.message.value shouldBe s"greet is called by user $name"
  }

}
