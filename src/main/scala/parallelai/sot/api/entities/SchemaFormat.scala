package parallelai.sot.api.entities

@deprecated(message = "Using SchemaType", since = "9th February 2018")
sealed trait SchemaFormat {
  def name: String
}

/*
object SchemaFormat {
  implicit class StringToSchermaFormat(s: String) {
    // TODO - Not nice
    def toSchemaFormat: SchemaFormat = s match {
      case "avro" => AvroSchemaFormat
      case "proto" => ProtoSchemaFormat
      case "bigquery" => BigQuerySchemaFormat
      case "json" => JsonSchemaFormat
      case "generic" => GenericSchemaFormat
      case _ => throw new UnsupportedOperationException(s"Schema Format name $s unknown")
    }
  }

  implicit object SchemaJsonFormat extends JsonFormat[SchemaFormat] {
    def write(obj: SchemaFormat) = JsString(obj.toString)

    def read(json: JsValue): SchemaFormat = json match {
      case JsString(s) => s.toSchemaFormat
      case _ => throw new UnsupportedOperationException("Formats can only be a String")
    }
  }

  implicit val entityMappableType: BaseDatastoreMappableType[SchemaFormat] = at[SchemaFormat](toE, fromE)

  private def toE(v: com.google.datastore.v1.Value): SchemaFormat = v.getStringValue.toSchemaFormat

  private def fromE(i: SchemaFormat): com.google.datastore.v1.Value =
    com.google.datastore.v1.Value.newBuilder().setStringValue(i.name).build()
}

case object AvroSchemaFormat extends SchemaFormat {
  override val name = "avro"
}

case object ProtoSchemaFormat extends SchemaFormat {
  override val name = "proto"
}

case object BigQuerySchemaFormat extends SchemaFormat {
  override val name = "bigquery"
}

case object JsonSchemaFormat extends SchemaFormat {
  override val name = "json"
}

case object GenericSchemaFormat extends SchemaFormat {
  override val name = "generic"
}*/
