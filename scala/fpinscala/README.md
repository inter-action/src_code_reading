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
      SGen(g andThen (_ map f))

== wildcard _
    ignore param
      eg.1
        case class SGen[+A](g: Int => Gen[A])

        implicit def unsized[A](g: Gen[A]): SGen[A] = SGen(_ => g)//ignore int param


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

