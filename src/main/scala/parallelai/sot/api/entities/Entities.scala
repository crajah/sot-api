package parallelai.sot.api.entities

// TODO - Remove
@deprecated(message = "Gradually splitting up until this file will no longer be needed", since = "9th February 2018")
object Entities {
  /*case class ExecutorInfo(version: String)

  object ExecutorInfo {
    implicit val rootJsonFormat: RootJsonFormat[ExecutorInfo] = jsonFormat1(ExecutorInfo.apply)
  }

  case class ExecutorActive(version: String, active: Boolean)

  object ExecutorActive {
    implicit val rootJsonFormat: RootJsonFormat[ExecutorActive] = jsonFormat2(ExecutorActive.apply)
  }*/

  /*case class Name(name: String)

  object Name {
    implicit val rootJsonFormat: RootJsonFormat[Name] = jsonFormat1(Name.apply)
  }*/

  // TODO - Are these required ???
  /*case class SchemaInfo(id: String, name: String, version: Int, format: SchemaFormat)

  object SchemaInfo {
    implicit val rootJsonFormat: RootJsonFormat[SchemaInfo] = jsonFormat4(SchemaInfo.apply)
  }

  case class SchemaConvert(id: String, version: Int, format: SchemaFormat)

  object SchemaConvert {
    implicit val rootJsonFormat: RootJsonFormat[SchemaConvert] = jsonFormat3(SchemaConvert.apply)
  }

  case class Schema(id: String, name: String, version: Int, format: SchemaFormat, schema: String, timestamp: Option[Instant] = Option(now)) extends TimedBaseEntity

  object Schema {
    implicit val rootJsonFormat: RootJsonFormat[Schema] = jsonFormat6(Schema.apply)
  }

  case class SchemaVersion(version: Int, format: SchemaFormat, schema: String)

  object SchemaVersion {
    implicit val rootJsonFormat: RootJsonFormat[SchemaVersion] = jsonFormat3(SchemaVersion.apply)
  }

  case class SchemaInstance(format: SchemaFormat, schema: String)

  object SchemaInstance {
    implicit val rootJsonFormat: RootJsonFormat[SchemaInstance] = jsonFormat2(SchemaInstance.apply)
  }*/

  /*case class KeyValue(key: String, value: String)

  object KeyValue {
    implicit val rootJsonFormat: RootJsonFormat[KeyValue] = jsonFormat2(KeyValue.apply)
  }*/

  /*case class Tap(id: String, config: List[KeyValue], timestamp: Option[Instant] = Option(now)) extends TimedBaseEntity

  object Tap {
    implicit val rootJsonFormat: RootJsonFormat[Tap] = jsonFormat3(Tap.apply)
  }*/
}