[original repo](https://github.com/fpinscala/fpinscala)

[manning: functional programming in scala](http://www.manning.com/bjarnason/)

# Desc
这本书是我目前最喜欢的书之一, 里边的内容新颖有趣。也是这本书让我真正感受到了 scala 的魅力所在。
* 强大而且复杂的类型系统，让我们在语言方便的抽象编程上提供了利器，让我们可以做到根据 法则(law) 来构建程序，而无需关注具体的类型。
  这么做的好处呢?这种方式会改变你写代码的思维, 通过 law 提供的法则作为你写逻辑的 drive。来构建更加抽象和简洁内聚高的程序。



####
    :?s: 语法标注
    :?:未解决的疑问
    :b: bookmark


# chapter notes

## chapter 11: ￼Monads
这章主要讲了函数式编程中的 Monads 和两个关键的 Monads Law: associative and identity law. 
这章也是理解 map flatMap 之类的函数的关键的一章

我个人关于 Monads 的理解就是任何符合 Monads 操作集的类型 (flatMap, Map, unit ....) Monads 只是这些操作集的抽象
和约束 (must comply the associative and identity law).

!! 注意这章源码的 stateMonad 的定义和用法, 有些特别

what is Monads:
>A monad is an implementation of one of the minimal sets of monadic combinators, satisfying the 
laws of associativity and identity.

about functors and Monads:
>The names functor and monad come from the branch of mathematics called category theory

>The Monad contract doesn’t specify what is happening between the lines, only that whatever
is happening satisfies the laws of associativity and identity.(so you can call `a.flatMap(=>b.flatMap())` 
with out breaking the logic)

primitive operations:
primitive operations is the minimal set of trait operations that depended on their implementator's implementation.
for example: monad's primitive operations is unit & flatMap

chapter notes:
>An abstract topic like this can’t be fully understood all at once. It requires an iterative 
approach where you keep revisiting the topic from different perspectives.


# NOTES

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

== scala for comprehension with flatMap and map

[scalas-for-comprehension-with-futures](http://stackoverflow.com/questions/19045936/scalas-for-comprehension-with-futures)

[!what-is-scalas-yield](http://stackoverflow.com/questions/1052476/what-is-scalas-yield)

>First about for comprehension. It was answered on SO many many times, that it's an abstraction over a couple 
of monadic operations: map, flatMap, withFilter. When you use <-, scalac desugars this lines into monadic flatMap:

    r1 <- result1 into result.flatMap(r1 => .... )

>it looks like an imperative computation (what a monad is all about), 
you bind a computation result to the r1. And yield part is desugared into map call. 
Result type depends on the type of result's.

>Future trait has a flatMap and map functions, so we can use for comprehension with it. 
In your example can be desugared into the following code:

    result1.flatMap(r1 => result2.flatMap(r2 => result3.map(r3 => r1 + r2 + r3) ) )

>BUT you should remember that this gonna be evaluated sequentially, not in parallel, 
because the result of result2 and result3 depends on r1. To make it parallel you should at 
first create future and then collect them in for comprehension, you can think of this like about a pipe:

    val result1 = future(...)
    val result2 = future(...)
    val result3 = future(...)

    val res = for {
       r1 <- result1
       r2 <- result2
       r3 <- result3
    } yield (r1+r2+r3)


== f.curried

    >val f = (A: Int, B:Int) => A+B
    f: (Int, Int) => Int = <function2>
    >f.curried
    Int => (Int => Int) = <function1>


== type lambda: @page201
A type constructor declared inline like this is often called a type lambda in Scala.

    Monad[({type IntState[A] = State[Int, A]})#IntState]

== Unit 
    here it use () to denote Unit return

    def setState[S](s: S): State[S,Unit] = State(_ => ((),s))

== case object
defined in State.scala
   
    case object Coin extends Input


# TODOS
== gen.scala: ** unapply 定义及用法

    object ** {
      def unapply[A,B](p: (A,B)) = Some(p)
    }


    def forAllPar3[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
      forAll(S ** g) { case s ** a => f(a)(s).get }

== Gen.scala 和 Exhaustive.scala 的区别

== Chapter 9
  Result 中 commit的作用

== Monad package defination
    package fpinscala
    package monads

parallelism/
  如何实现并行计算的

review this book's code

#### Actor.scala中得数据结构状态示例
![Actor.scala中得数据结构状态示例](img/non-intrusive-mpsc-node-based-queue.png?raw=true "Actor.scala中得数据结构状态示例")

