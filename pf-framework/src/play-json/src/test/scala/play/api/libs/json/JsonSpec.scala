/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import org.specs2.mutable._

import com.fasterxml.jackson.databind.JsonNode

import play.api.libs.functional.syntax._
import play.api.libs.json.Json._

object JsonSpec extends Specification {
  case class User(id: Long, name: String, friends: List[User])

  implicit val UserFormat: Format[User] = (
    (__ \ 'id).format[Long] and
    (__ \ 'name).format[String] and
    (__ \ 'friends).lazyFormat(Reads.list(UserFormat), Writes.list(UserFormat))
  )(User, unlift(User.unapply))

  case class Car(id: Long, models: Map[String, String])

  implicit val CarFormat = (
    (__ \ 'id).format[Long] and
    (__ \ 'models).format[Map[String, String]]
  )(Car, unlift(Car.unapply))

  import java.util.Date
  import java.text.SimpleDateFormat
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'" // Iso8601 format (forgot timezone stuff)
  val dateParser = new SimpleDateFormat(dateFormat)

  case class Post(body: String, created_at: Option[Date])

  implicit val PostFormat = (
    (__ \ 'body).format[String] and
    (__ \ 'created_at).formatNullable[Option[Date]](
      Format(
        Reads.optionWithNull(Reads.dateReads(dateFormat)),
        Writes.optionWithNull(Writes.dateWrites(dateFormat))
      )
    ).inmap(optopt => optopt.flatten, (opt: Option[Date]) => Some(opt))
  )(Post, unlift(Post.unapply))

  "JSON" should {
    "equals JsObject independently of field order" in {
      Json.obj(
        "field1" -> 123,
        "field2" -> "beta",
        "field3" -> Json.obj(
          "field31" -> true,
          "field32" -> 123.45,
          "field33" -> Json.arr("blabla", 456L, JsNull)
        )
      ) must beEqualTo(
          Json.obj(
            "field2" -> "beta",
            "field3" -> Json.obj(
              "field31" -> true,
              "field33" -> Json.arr("blabla", 456L, JsNull),
              "field32" -> 123.45
            ),
            "field1" -> 123
          )
        )

      Json.obj(
        "field1" -> 123,
        "field2" -> "beta",
        "field3" -> Json.obj(
          "field31" -> true,
          "field32" -> 123.45,
          "field33" -> Json.arr("blabla", JsNull)
        )
      ) must not equalTo (
          Json.obj(
            "field2" -> "beta",
            "field3" -> Json.obj(
              "field31" -> true,
              "field33" -> Json.arr("blabla", 456L),
              "field32" -> 123.45
            ),
            "field1" -> 123
          )
        )

      Json.obj(
        "field1" -> 123,
        "field2" -> "beta",
        "field3" -> Json.obj(
          "field31" -> true,
          "field32" -> 123.45,
          "field33" -> Json.arr("blabla", 456L, JsNull)
        )
      ) must not equalTo (
          Json.obj(
            "field3" -> Json.obj(
              "field31" -> true,
              "field33" -> Json.arr("blabla", 456L, JsNull),
              "field32" -> 123.45
            ),
            "field1" -> 123
          )
        )
    }

    "serialize and deserialize maps properly" in {
      val c = Car(1, Map("ford" -> "1954 model"))
      val jsonCar = toJson(c)

      jsonCar.as[Car] must equalTo(c)
    }
    "serialize and deserialize" in {
      val luigi = User(1, "Luigi", List())
      val kinopio = User(2, "Kinopio", List())
      val yoshi = User(3, "Yoshi", List())
      val mario = User(0, "Mario", List(luigi, kinopio, yoshi))
      val jsonMario = toJson(mario)
      jsonMario.as[User] must equalTo(mario)
    }
    "Complete JSON should create full Post object" in {
      val postJson = """{"body": "foobar", "created_at": "2011-04-22T13:33:48Z"}"""
      val expectedPost = Post("foobar", Some(dateParser.parse("2011-04-22T13:33:48Z")))
      val resultPost = Json.parse(postJson).as[Post]
      resultPost must equalTo(expectedPost)
    }
    "Optional parameters in JSON should generate post w/o date" in {
      val postJson = """{"body": "foobar"}"""
      val expectedPost = Post("foobar", None)
      val resultPost = Json.parse(postJson).as[Post]
      resultPost must equalTo(expectedPost)
    }
    "Invalid parameters shoud be ignored" in {
      val postJson = """{"body": "foobar", "created_at":null}"""
      val expectedPost = Post("foobar", None)
      val resultPost = Json.parse(postJson).as[Post]
      resultPost must equalTo(expectedPost)
    }

    "Serialize long integers correctly" in {
      val t = 1330950829160L
      val m = Map("timestamp" -> t)
      val jsonM = toJson(m)
      (jsonM \ "timestamp").as[Long] must_== t
      jsonM.toString must_== "{\"timestamp\":1330950829160}"
    }

    "Serialize short integers correctly" in {
      val s: Short = 1234
      val m = Map("s" -> s)
      val jsonM = toJson(m)
      (jsonM \ "s").as[Short] must_== s
      jsonM.toString must_== "{\"s\":1234}"
    }

    "Serialize bytes correctly" in {
      val b: Byte = 123
      val m = Map("b" -> b)
      val jsonM = toJson(m)
      (jsonM \ "b").as[Byte] must_== b
      jsonM.toString must_== "{\"b\":123}"
    }

    "Serialize and deserialize BigDecimals" in {
      val n = BigDecimal("12345678901234567890.42")
      val json = toJson(n)
      json must equalTo(JsNumber(n))
      fromJson[BigDecimal](json) must equalTo(JsSuccess(n))
    }

    "Not lose precision when parsing BigDecimals" in {
      val n = BigDecimal("12345678901234567890.123456789")
      val json = toJson(n)
      parse(stringify(json)) must equalTo(json)

    }

    "Not lose precision when parsing big integers" in {
      // By big integers, we just mean integers that overflow long, since Jackson has different code paths for them
      // from decimals
      val i = BigDecimal("123456789012345678901234567890")
      val json = toJson(i)
      parse(stringify(json)) must equalTo(json)
    }

    "Serialize and deserialize Lists" in {
      val xs: List[Int] = (1 to 5).toList
      val json = arr(1, 2, 3, 4, 5)
      toJson(xs) must equalTo(json)
      fromJson[List[Int]](json) must equalTo(JsSuccess(xs))
    }

    "Serialize and deserialize Jackson ObjectNodes" in {
      val on = JacksonJson.mapper.createObjectNode()
        .put("foo", 1).put("bar", "two")
      val json = Json.obj("foo" -> 1, "bar" -> "two")
      toJson(on) must equalTo(json)
      fromJson[JsonNode](json).map(_.toString) must_== JsSuccess(on.toString)
    }

    "Serialize and deserialize Jackson ArrayNodes" in {
      val an = JacksonJson.mapper.createArrayNode()
        .add("one").add(2)
      val json = Json.arr("one", 2)
      toJson(an) must equalTo(json)
      fromJson[JsonNode](json).map(_.toString) must_== JsSuccess(an.toString)
    }

    "Map[String,String] should be turned into JsValue" in {
      val f = toJson(Map("k" -> "v"))
      f.toString must equalTo("{\"k\":\"v\"}")
    }

    "Can parse recursive object" in {
      val recursiveJson = """{"foo": {"foo":["bar"]}, "bar": {"foo":["bar"]}}"""
      val expectedJson = JsObject(List(
        "foo" -> JsObject(List(
          "foo" -> JsArray(List[JsValue](JsString("bar")))
        )),
        "bar" -> JsObject(List(
          "foo" -> JsArray(List[JsValue](JsString("bar")))
        ))
      ))
      val resultJson = Json.parse(recursiveJson)
      resultJson must equalTo(expectedJson)

    }
    "Can parse null values in Object" in {
      val postJson = """{"foo": null}"""
      val parsedJson = Json.parse(postJson)
      val expectedJson = JsObject(List("foo" -> JsNull))
      parsedJson must equalTo(expectedJson)
    }
    "Can parse null values in Array" in {
      val postJson = """[null]"""
      val parsedJson = Json.parse(postJson)
      val expectedJson = JsArray(List(JsNull))
      parsedJson must equalTo(expectedJson)
    }

    "JSON pretty print" in {
      val js = Json.obj(
        "key1" -> "toto",
        "key2" -> Json.obj("key21" -> "tata", "key22" -> 123),
        "key3" -> Json.arr(1, "tutu")
      )

      Json.prettyPrint(js) must beEqualTo("""{
  "key1" : "toto",
  "key2" : {
    "key21" : "tata",
    "key22" : 123
  },
  "key3" : [ 1, "tutu" ]
}""")
    }

    "null root object should be parsed as JsNull" in {
      parse("null") must_== JsNull
    }

    "asciiStringify should escape non-ascii characters" in {
      val js = Json.obj(
        "key1" -> "\u2028\u2029\u2030",
        "key2" -> "\u00E1\u00E9\u00ED\u00F3\u00FA",
        "key3" -> "\u00A9\u00A3",
        "key4" -> "\u6837\u54C1"
      )
      Json.asciiStringify(js) must beEqualTo(
        "{\"key1\":\"\\u2028\\u2029\\u2030\"," +
          "\"key2\":\"\\u00E1\\u00E9\\u00ED\\u00F3\\u00FA\"," +
          "\"key3\":\"\\u00A9\\u00A3\"," + "" +
          "\"key4\":\"\\u6837\\u54C1\"}")
    }

    "asciiStringify should escape ascii characters properly" in {
      val js = Json.obj(
        "key1" -> "ab\n\tcd",
        "key2" -> "\"\r"
      )
      Json.asciiStringify(js) must beEqualTo("""{"key1":"ab\n\tcd","key2":"\"\r"}""")
    }

    "parse from InputStream" in {
      val js = Json.obj(
        "key1" -> "value1",
        "key2" -> true,
        "key3" -> JsNull,
        "key4" -> Json.arr(1, 2.5, "value2", false, JsNull),
        "key5" -> Json.obj(
          "key6" -> "こんにちは",
          "key7" -> BigDecimal("12345678901234567890.123456789")
        )
      )
      val stream = new java.io.ByteArrayInputStream(js.toString.getBytes("UTF-8"))
      Json.parse(stream) must beEqualTo(js)
    }
  }

  "JSON Writes" should {
    "write list/seq/set/map" in {
      import Writes._

      Json.toJson(List(1, 2, 3)) must beEqualTo(Json.arr(1, 2, 3))
      Json.toJson(Set("alpha", "beta", "gamma")) must beEqualTo(Json.arr("alpha", "beta", "gamma"))
      Json.toJson(Seq("alpha", "beta", "gamma")) must beEqualTo(Json.arr("alpha", "beta", "gamma"))
      Json.toJson(Map("key1" -> "value1", "key2" -> "value2")) must beEqualTo(Json.obj("key1" -> "value1", "key2" -> "value2"))

      implicit val myWrites = (
        (__ \ 'key1).write(constraints.list[Int]) and
        (__ \ 'key2).write(constraints.set[String]) and
        (__ \ 'key3).write(constraints.seq[String]) and
        (__ \ 'key4).write(constraints.map[String])
      ).tupled

      Json.toJson((
        List(1, 2, 3),
        Set("alpha", "beta", "gamma"),
        Seq("alpha", "beta", "gamma"),
        Map("key1" -> "value1", "key2" -> "value2")
      )) must beEqualTo(
        Json.obj(
          "key1" -> Json.arr(1, 2, 3),
          "key2" -> Json.arr("alpha", "beta", "gamma"),
          "key3" -> Json.arr("alpha", "beta", "gamma"),
          "key4" -> Json.obj("key1" -> "value1", "key2" -> "value2")
        )
      )
    }

    "write in 2nd level" in {
      case class TestCase(id: String, attr1: String, attr2: String)

      val js = Json.obj(
        "id" -> "my-id",
        "data" -> Json.obj(
          "attr1" -> "foo",
          "attr2" -> "bar"
        )
      )

      implicit val testCaseWrites: Writes[TestCase] = (
        (__ \ "id").write[String] and
        (__ \ "data" \ "attr1").write[String] and
        (__ \ "data" \ "attr2").write[String]
      )(unlift(TestCase.unapply))

      Json.toJson(TestCase("my-id", "foo", "bar")) must beEqualTo(js)

    }
  }

}

