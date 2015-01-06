/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import javax.inject.Inject

import com.google.inject.Guice
import org.specs2.mutable.Specification
import play.api.{ PlayException, Configuration, Environment }
import play.api.inject.Binding
import play.api.inject.guice.GuiceApplicationLoader

import scala.reflect.ClassTag

object ReflectSpec extends Specification {

  "Reflect" should {

    "load bindings from configuration" in {

      "return no bindings for provided configuration" in {
        bindings("provided", "none") must beEmpty
      }

      "return the default implementation when none configured or default class doesn't exist" in {
        doQuack(bindings("", "NoDuck")) must_== "quack"
      }

      "return a default Scala implementation" in {
        doQuack(bindings[CustomDuck]("")) must_== "custom quack"
      }

      "return a default Java implementation" in {
        doQuack(bindings[CustomJavaDuck]("")) must_== "java quack"
      }

      "return a configured Scala implementation" in {
        doQuack(bindings(classOf[CustomDuck].getName, "NoDuck")) must_== "custom quack"
      }

      "return a configured Java implementation" in {
        doQuack(bindings(classOf[CustomJavaDuck].getName, "NoDuck")) must_== "java quack"
      }

      "throw an exception if a configured class doesn't exist" in {
        doQuack(bindings[CustomDuck]("NoDuck")) must throwA[PlayException]
      }

      "throw an exception if a configured class doesn't implement either of the interfaces" in {
        doQuack(bindings[CustomDuck](classOf[NotADuck].getName)) must throwA[PlayException]
      }

    }
  }

  def bindings(configured: String, defaultClassName: String): Seq[Binding[_]] = {
    Reflect.bindingsFromConfiguration[Duck, JavaDuck, JavaDuckAdapter, DefaultDuck](
      Environment.simple(), Configuration.from(Map("duck" -> configured)), "duck", defaultClassName)
  }

  def bindings[Default: ClassTag](configured: String): Seq[Binding[_]] = {
    bindings(configured, implicitly[ClassTag[Default]].runtimeClass.getName)
  }

  trait Duck {
    def quack: String
  }

  trait JavaDuck {
    def getQuack: String
  }

  class JavaDuckAdapter @Inject() (underlying: JavaDuck) extends Duck {
    def quack = underlying.getQuack
  }

  class DefaultDuck extends Duck {
    def quack = "quack"
  }

  class CustomDuck extends Duck {
    def quack = "custom quack"
  }

  class CustomJavaDuck extends JavaDuck {
    def getQuack = "java quack"
  }

  class NotADuck

  def doQuack(bindings: Seq[Binding[_]]): String = {
    Guice.createInjector(GuiceApplicationLoader.guiced(bindings)).getInstance(classOf[Duck]).quack
  }

}
