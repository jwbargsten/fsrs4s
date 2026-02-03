package org.bargsten.fsrs

// import scala.util.chaining.*

object util {

  // https://contributors.scala-lang.org/t/new-function-pipeif-proposal/5223/3
  extension [A](a: A) {
    def cond[B >: A](pf: PartialFunction[A, B]): B = pf.applyOrElse(a, identity)
  }
}
