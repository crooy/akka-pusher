package com.github.dtaniwaki.akka_pusher

import akka.actor._
import spray.json.{JsonFormat,JsString, JsValue, JsonWriter}
import scala.concurrent.{ Future, Await, Awaitable }
import scala.concurrent.duration._
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContext.Implicits.global

import PusherModels.ChannelData

class PusherActor extends Actor with StrictLogging {
  implicit val system = ActorSystem("pusher")

  implicit object jsValueJsonFormat extends JsonWriter[JsValue] {
    override def write(obj: JsValue): JsValue = obj
  }

  val pusher = new PusherClient()

  override def receive: Receive = {
    case TriggerMessage(event, channel, message, socketId) =>
      sender ! new ResponseMessage(Await.result(pusher.trigger(event, channel, message, socketId), 5 seconds))
    case ChannelMessage(channel, attributes) =>
      sender ! new ResponseMessage(Await.result(pusher.channel(channel, attributes), 5 seconds))
    case ChannelsMessage(prefixFilter, attributes) =>
      sender ! new ResponseMessage(Await.result(pusher.channels(prefixFilter, attributes), 5 seconds))
    case UsersMessage(channel) =>
      sender ! new ResponseMessage(Await.result(pusher.users(channel), 5 seconds))
    case AuthenticateMessage(channel, socketId, data) =>
      sender ! new ResponseMessage(pusher.authenticate(channel, socketId, data))
    case ValidateSignatureMessage(key, signature, body) =>
      sender ! new ResponseMessage(pusher.validateSignature(key, signature, body))
    case message =>
      logger.info(s"Unknown event: $message")
  }

  override def postStop() = {
    super.postStop()
    pusher.shutdown()
  }
}

object PusherActor {
  def props(): Props = Props(new PusherActor())
}
