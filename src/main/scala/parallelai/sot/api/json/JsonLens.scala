package parallelai.sot.api.json

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{ Either, Left, Right }
import spray.json._

object JsonLens {
  implicit def jsValue2Lens(value: JsValue): Json = Json(Option(value))

  implicit def lens2jsValue(value: Json): JsValue = value.unwrap

  class Json(val value: Option[JsValue]) {
    def /(name: String): Json = Json(value.flatMap(_.asJsObject.fields.get(name)))

    def -(name: String): Json = Json(value.map(obj => JsObject(obj.asJsObject.fields - name)))

    def apply(index: Int): Json = value map {
      case array: JsArray if array.elements.size > index => Json(Some(array.elements(index)))
      case _ => Json.empty
    } getOrElse Json.empty

    def isDefined: Boolean = value.isDefined

    def isEmpty: Boolean = value.isEmpty

    def at(path: String, separator: Char): Json = path.split(separator).foldLeft(this) {
      case (json, segment) => json / segment
    }

    def at(path: String): Json = at(path, '/')

    def optionAt[T](path: String, separator: Char)(implicit r: JsonReader[T]): Option[T] =
      at(path, separator).value flatMap {
        case JsNull => None
        case v => Some(v.convertTo[T])
      }

    def optionAt[T](path: String)(implicit r: JsonReader[T]): Option[T] = optionAt[T](path, '/')

    def hasPath(path: String, separator: Char): Boolean = at(path, separator).isDefined

    def hasPath(path: String): Boolean = hasPath(path, '/')

    def unwrap: JsValue = value.getOrElse(JsNull)

    def unwrapTo[T <: JsValue]: T = value.get.asInstanceOf[T]

    def foldLeft[A](z: A)(f: (A, JsValue) => A): A = value match {
      case Some(array: JsArray) => array.elements.foldLeft(z)(f)
      case _ => z
    }

    def foreach[A](f: JsValue => A): Unit = map(f).foreach(_ => ())

    def map[A](f: JsValue => A): Seq[A] = value match {
      case Some(array: JsArray) => array.elements.map(f)
      case Some(jsvalue) => Seq(f(jsvalue))
      case _ => Seq.empty
    }

    def flatMap(f: JsValue => Json): Json = value match {
      case Some(array: JsArray) => Json(JsArray(array.map(f.andThen(_.unwrap)): _*))
      case Some(jsvalue) => f(jsvalue)
      case _ => Json.empty
    }

    def filter(f: JsValue => Boolean): Json = value match {
      case Some(array: JsArray) => Json(JsArray(array.elements.filter(f)))
      case Some(JsObject(fields)) => Json(JsObject(fields.filter { case (_, v) => f(v) }))
      case Some(jsvalue) => if (f(jsvalue)) this else Json.empty
      case _ => Json.empty
    }

    def filterNot(f: JsValue => Boolean): Json = filter(!f(_))

    def filterObject(f: JsField => Boolean): Json = value match {
      case Some(JsObject(fields)) => Json(JsObject(fields.filter(f)))
      case _ => Json.empty
    }

    def getOrElse(that: JsValue): JsValue = value getOrElse that

    def orElse(that: Json): Json = if (isDefined) this else that

    override def equals(other: Any): Boolean = other match {
      case json: Json => value == json.value
      case _ => false
    }

    /** Add an array element. */
    def :+(update: Any): Json = {
      def encoded: JsValue = Json.encode(update) match {
        case Left(_) => throw new IllegalArgumentException("attempt to add a JsField to a JsArray")
        case Right(element) => element
      }

      value match {
        case Some(JsArray(elements)) => Json(JsArray(elements :+ encoded))
        case Some(JsNull) | None => Json(JsArray(encoded))
        case _ => throw new IllegalArgumentException("attempt to add an array element to a JsObject")
      }
    }

    //---- Add/update object fields

    def <<(name: String, update: String): Json = updated(name -> JsString(update))

    def <<(name: String, update: Boolean): Json = updated(name -> JsBoolean(update))

    def <<(name: String, update: Int): Json = updated(name -> JsNumber(update))

    def <<(name: String, update: Long): Json = updated(name -> JsNumber(update))

    def <<(name: String, update: Double): Json = updated(name -> JsNumber(update))

    def <<(name: String, update: BigInt): Json = updated(name -> JsNumber(update))

    def <<(name: String, update: BigDecimal): Json = updated(name -> JsNumber(update))

    def <<(name: String, update: JsValue): Json = updated(name -> update)

    def <<(nameAndValue: (String, JsValue)): Json = updated(nameAndValue._1 -> nameAndValue._2)

    def <<(name: String, update: Json): Json = updated(name -> update.unwrap)

    /**
     * Return an updated copy of the JSON, removing `name` from objects if the option is empty.
     *
     * Use [[<<?]] to only apply a non-empty option.
     */
    def <<[T: ClassTag: JsonWriter](name: String, option: Option[T]): Json =
      option map { value =>
        updated((name, implicitly[JsonWriter[T]].write(value)))
      } getOrElse value match {
        case Some(JsObject(fields)) => Json(JsObject(fields - name))
        case _ => this
      }

    /**
     * This will handle *uniform* iterable update values with regard to `JsField`s.
     * What that means is that if `update` contains a `Tuple2[String, _]` for a JsField,
     * then all elements must be field tuples, and a JsObject will be returned.
     *
     * Otherwise, a JsArray of values will be returned.
     */
    def <<(name: String, update: Iterable[_], encodeNulls: Boolean = false): Json =
      updated(name -> Json.array(update, encodeNulls).toJsArray)

    def updated(update: (String, JsValue)): Json =
      if (value.isEmpty) Json(JsObject(Map(update)))
      else Json(JsObject(value.get.asJsObject.fields + update))

    //---- Conditionally update from an option

    /**
     * Return an updated copy of the JSON if the option is defined.
     *
     * Use [[<<]] to remove `name` with empty options.
     */
    def <<?[T: ClassTag: JsonWriter](name: String, option: Option[T]): Json =
      option map { value =>
        updated(name -> implicitly[JsonWriter[T]].write(value))
      } getOrElse this

    def toBoolean: Boolean = value match {
      case Some(JsBoolean(bool)) => bool
      case _ => false
    }

    def toJsObject: JsObject = value match {
      case Some(jsobj: JsObject) => jsobj
      case Some(JsNull) | None => JsObject.empty
      case Some(jsvalue) => JsObject("value" -> jsvalue)
    }

    def toJsArray: JsArray = value match {
      case Some(jsarray: JsArray) => jsarray
      case Some(JsNull) | None => JsArray.empty
      case Some(jsvalue) => JsArray(jsvalue)
    }

    override def toString: String = value match {
      case Some(JsString(s)) => s
      case Some(JsNumber(n)) => n.toString
      case Some(v) => v.prettyPrint
      case _ => ""
    }
  }

  object Json {
    val empty: Json = new Json(None)

    def apply(value: JsValue) = new Json(Some(value))

    def apply(value: Option[JsValue]) = new Json(value)

    def apply(values: JsField*) = new Json(Some(JsObject(values: _*)))

    def unapply(lens: Json): Option[JsValue] = lens.value

    def array(values: Iterable[_], encodeNulls: Boolean = false): Json = {
      def encodeValue(items: Iterable[_]): JsValue = {
        var fields = Vector.empty[JsField]
        var elements = Vector.empty[JsValue]

        items foreach (encode(_) match {
          case Right(JsNull) => if (encodeNulls) elements = elements :+ JsNull
          case Right(element) => elements = elements :+ element
          case Left(field) => if (encodeNulls || field._2 != JsNull) fields = fields :+ field
        })

        if (elements.nonEmpty || fields.isEmpty) JsArray(elements: _*)
        else JsObject(fields: _*)
      }

      encodeValue(values)
    }

    def encode(anyItem: Any): Either[JsField, JsValue] = anyItem match {
      case null | () => Right(JsNull)
      case item: JsValue => Right(item)
      case item: Json => Right(item.unwrap)
      case item: (_, _) if item._1.isInstanceOf[String] => Left(item._1.toString -> encode(item._2).right.get)
      case item: Boolean => Right(JsBoolean(item))
      case item: Short => Right(JsNumber(item))
      case item: Int => Right(JsNumber(item))
      case item: Long => Right(JsNumber(item))
      case item: Float => Right(JsNumber(item))
      case item: Double => Right(JsNumber(item))
      case item: BigInt => Right(JsNumber(item))
      case item: BigDecimal => Right(JsNumber(item))
      case item => Right(JsString(item.toString))
    }
  }
}