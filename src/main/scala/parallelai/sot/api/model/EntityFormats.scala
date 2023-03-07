package parallelai.sot.api.model

import shapeless.datatype.datastore._
import shapeless.datatype.datastore.DatastoreType.at
import spray.json._
import com.google.datastore.v1.Value
import com.google.datastore.v1.Value._
import parallelai.common.persist.Identity
import parallelai.sot.executor.model.SOTMacroConfig._
import spray.json.lenses.JsonLenses._

object EntityFormats extends EntityFormats

trait EntityFormats extends JsonFormats {
  implicit val j: Identity[JsValue] =
    Identity[JsValue](_.extract[String]("id"))

  implicit val ie: Identity[IdentityEntity] =
    Identity[IdentityEntity](_.id)

  implicit val entityToJson: Value => JsValue =
    _.getStringValue.parseJson

  implicit val jsonToEntity: JsValue => Value = { json =>
    newBuilder setStringValue json.compactPrint build
  }

  implicit val jsonMappableType: BaseDatastoreMappableType[JsValue] =
    at[JsValue](entityToJson, jsonToEntity)

  implicit val entityToDefinition: Value => Definition =
    _.getStringValue.parseJson.convertTo[Definition]

  implicit val definitionToEntity: Definition => Value = { definition =>
    newBuilder setStringValue definition.toJson.compactPrint build
  }

  implicit val definitionMappableType: BaseDatastoreMappableType[Definition] =
    at[Definition](entityToDefinition, definitionToEntity)

  implicit val entityToTransformationOp: Value => TransformationOp =
    _.getStringValue.parseJson.convertTo[TransformationOp]

  implicit val transformationOpToEntity: TransformationOp => Value = { transformationOp =>
    newBuilder setStringValue transformationOp.toJson.compactPrint build
  }

  implicit val transformationOpMappableType: BaseDatastoreMappableType[TransformationOp] =
    at[TransformationOp](entityToTransformationOp, transformationOpToEntity)

  implicit val entityToLookup: Value => DatastoreLookupDefinition =
    _.getStringValue.parseJson.convertTo[DatastoreLookupDefinition]

  implicit val lookupToEntity: DatastoreLookupDefinition => Value = { lookup =>
    newBuilder setStringValue lookup.toJson.compactPrint build
  }

  implicit val lookupMappableType: BaseDatastoreMappableType[DatastoreLookupDefinition] =
    at[DatastoreLookupDefinition](entityToLookup, lookupToEntity)

  // TODO - Make generic instead of hardcoded to String
  implicit val entityToSeqOfSeq: Value => Seq[Seq[String]] =
    _.getStringValue.parseJson.convertTo[Seq[Seq[String]]]

  implicit val seqOfSeqToEntity: Seq[Seq[String]] => Value = { seqs =>
    newBuilder setStringValue seqs.toJson.compactPrint build
  }

  implicit val SeqsMappableType: BaseDatastoreMappableType[Seq[Seq[String]]] =
    at[Seq[Seq[String]]](entityToSeqOfSeq, seqOfSeqToEntity)
}