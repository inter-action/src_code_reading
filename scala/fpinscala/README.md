[orgin repo](https://github.com/fpinscala/fpinscala)

[manning: functional programming in scala](http://www.manning.com/bjarnason/)

####
    :?s: 语法标注
    :?:未解决的疑问
    :b: bookmark

#### NOTES

== scala for

    // ----- Gen.scala ----------
    def sameParity(from: Int, to: Int): Gen[(Int,Int)] = for {
      i <- choose(from,to)
      j <- if (i%2 == 0) even(from,to) else odd(from,to)
    } yield (i,j)

== andThen

    // Gen.scala
    def map[B](f: A => B): SGen[B] =
      // :?: 这个地方的 _ 的用法比较隐晦, 我也不是十分确定,
      // 我猜是 g(Int)=>Gen 之后再用生成的 Gen示例调用 map(f) 返回一个新的 Gen示例
      // 确实是这样的. scala andThen 的定义 def andThen[A](g: (R) => A): (T1) => A, R 是第一个函数的返回结果.
      SGen(g andThen (_ map f))

== wildcard _
    ignore param
      eg.1
        case class SGen[+A](g: Int => Gen[A])

        implicit def unsized[A](g: Gen[A]): SGen[A] = SGen(_ => g)//ignore int param

== ignore imports
[stackoverflow, scala ignore imports](http://stackoverflow.com/questions/2871822/how-do-i-exclude-rename-some-classes-from-import-in-scala)

    import scala.collection.mutable.{Map => _, Set => _, _} //import all but Map & Set

== xx
    通过阅读代码, Parsers.scala, Gen.scala etc.这些写法通过 flatMap map 函数将计算的过程和状态(以case class)的形式反复封装到一个
    目标对象中(通常以 type 定义的 closure). 并利用 scala 的 lazy 的各种方式 (lazy val, s: => x, closure)等等方式延迟计算和协助
    计算过程的封装演变。

== inner class call outer trait methods
    // Parsers.scala
    trait X { self =>
      def add(x: Int):Int

      case class Y {
        def add3() = self.add(3)
      }
    }

#### TODOS
== gen.scala: ** unapply 定义及用法

    object ** {
      def unapply[A,B](p: (A,B)) = Some(p)
    }


    def forAllPar3[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
      forAll(S ** g) { case s ** a => f(a)(s).get }

== Gen.scala 和 Exhaustive.scala 的区别

== Chapter 9
  Result 中 commit的作用


#### Actor.scala中得数据结构状态示例
![Actor.scala中得数据结构状态示例](img/non-intrusive-mpsc-node-based-queue.png?raw=true "Actor.scala中得数据结构状态示例")

