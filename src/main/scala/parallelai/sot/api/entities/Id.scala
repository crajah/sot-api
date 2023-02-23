package parallelai.sot.api.entities

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class Id(value: String) extends AnyVal

object Id {
  implicit val rootJsonFormat: RootJsonFormat[Id] = jsonFormat(Id.apply, "id")
}

/*
case class IdForName(name: String, id: String)

object IdForName {
  implicit val rootJsonFormat: RootJsonFormat[IdForName] = jsonFormat2(IdForName.apply)
}*/
