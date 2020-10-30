package com.intenthq.action_processor.integrationsV2

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.{runtimeMirror, termNames, typeOf, weakTypeOf}

trait ReflectionHelpers {

  protected val classLoaderMirror: universe.Mirror = runtimeMirror(getClass.getClassLoader)

  /**
   * Encapsulates functionality to reflectively invoke the constructor
   * for a given case class type `T`.
   *
   * @tparam T the type of the case class this factory builds
   */
  class CaseClassFactory[T <: Product] {

    val tpe: universe.Type = weakTypeOf[T]
    val classSymbol: universe.ClassSymbol = tpe.typeSymbol.asClass

    if (!(tpe <:< typeOf[Product] && classSymbol.isCaseClass))
      throw new IllegalArgumentException(
        "CaseClassFactory only applies to case classes!"
      )

    val classMirror: universe.ClassMirror = classLoaderMirror reflectClass classSymbol

    val constructorSymbol: universe.Symbol = tpe.decl(termNames.CONSTRUCTOR)

    val defaultConstructor: universe.MethodSymbol =
      if (constructorSymbol.isMethod) constructorSymbol.asMethod
      else {
        val ctors = constructorSymbol.asTerm.alternatives
        ctors.map(_.asMethod).find(_.isPrimaryConstructor).get
      }

    val constructorMethod: universe.MethodMirror = classMirror reflectConstructor defaultConstructor

    /**
     * Attempts to create a new instance of the specified type by calling the
     * constructor method with the supplied arguments.
     *
     * @param args the arguments to supply to the constructor method
     */
    def buildWith(args: Seq[_]): T = constructorMethod(args: _*).asInstanceOf[T]
  }
}

object ReflectionHelpers extends ReflectionHelpers
