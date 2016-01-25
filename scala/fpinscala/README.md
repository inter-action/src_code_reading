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

What is Functors:

what can we benefit from Monads:

[what is Monads]:
>A monad is an implementation of one of the minimal sets of monadic combinators, satisfying the
laws of associativity and identity.

[Monad Laws]:

* associativity:

    op(op(a, b), c) == op(a, op(b, c))

* identity:

    M.map(Ma)(a => a) = Ma


[about functors and Monads]:
>The names functor and monad come from the branch of mathematics called category theory

>The Monad contract doesn’t specify what is happening between the lines, only that whatever
is happening satisfies the laws of associativity and identity.(so you can call `a.flatMap(=>b.flatMap())`
with out breaking the logic)

[primitive operations]:
primitive operations is the minimal set of trait operations that depended on their implementator's implementation.
for example: monad's primitive operations is unit & flatMap

[The Primitive Combinators of Monads]:
unit & flatMap



[chapter notes]:
>An abstract topic like this can’t be fully understood all at once. It requires an iterative
approach where you keep revisiting the topic from different perspectives.


## chapter 12: Applicative and traversable functors

@page 206
We’ll see that this new abstraction, called an applicative functor, is less powerful than a monad,
but we’ll also see that limitations come with benefits.

what is Applicative:

Applicative vs Monads:@page211

all Monads are applicative, the other way is not true.


scala vector

override def apply[A,B](m1: M)(m2: M): M = M.op(m1, m2)

exercise 12.7-12.11, 12.15

代码中 Monad 为什么要重写 apply, map2 方法

zipWithIndex_ 的 get[Int] 哪定义的？ 应该是 import state, State 里边的

sys.error("zip: Incompatible shapes.")

monoidApplicative why this has to be implicit

这两个的区别
    // G: Applicative[G] 的第一个 G 是 Applicative[G] 的实例
    def product[G[_]](G: Applicative[G])

    // 这里的 G[_] 中的 G 是 Type, implicit G: Monad[G] 中的第一个 G 表示 Mond[G] 的一个实例
    // 只不过这里有 implicit 关键字, or 这是对 G 的一个限定？
    def composeM[G[_],H[_]](implicit G: Monad[G], H: Monad[H], T: Traverse[H])



[The applicative laws]:

* Left and right identity:

        map2(unit(()), fa)((_,a) => a) == fa
        map2(fa, unit(()))((a,_) => a) == fa

* Associativity:

    // 满足

        op(a, op(b, c)) == op(op(a, b), c)

    //根据

        def product[A,B](fa: F[A], fb: F[B]): F[(A,B)]
        def assoc[A,B,C](p: (A,(B,C))): ((A,B), C) = p match { case (a, (b, c)) => ((a,b), c) }

    //推导出

        product(product(fa,fb),fc) == map(product(fa, product(fb,fc)))(assoc)

* Naturality of product:

    //满足

        map2(a,b)(productF(f,g)) == product(map(a)(f), map(b)(g))

        def productF[I,O,I2,O2](f: I => O, g: I2 => O2): (I,I2) => (O,O2) = (i,i2) => (f(i), g(i2))

what use are those Applicative Laws:
>The applicative laws are not surprising or profound. Just like the monad laws,
these are simple sanity checks that the applicative functor works in the way that we’d expect.


[The Primitive Combinators of Applicative]:

* product, map, and unit are an alternate formulation of Applicative.

        def product[A,B](fa: F[A], fb: F[B]): F[(A,B)
        def map[A, B](fa: F[A])(f: A => B): F[B]

* map2, unit


[Others]:

注意 Traverse trait 中 traverse 的写法. 还有 @map 的写法, a tricky one to understand

注意这章 State 的用法(zipWithIndex_, toList_), 很有意思. 第一个是当做计数器来用, 第二个是当做临时储存的容器再用



## chapter 13: External effects and I/O
desc:

[Todos]:

@Monad.scala

  case h #:: t => f(z,h) flatMap (z2 => foldM(t)(z2)(f))

















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

== compose vs andThen
  f compose g => f(g(x))
  f andThen g = g(f(x))


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

review this whole book & code

#### Actor.scala中得数据结构状态示例
![Actor.scala中得数据结构状态示例](img/non-intrusive-mpsc-node-based-queue.png?raw=true "Actor.scala中得数据结构状态示例")

