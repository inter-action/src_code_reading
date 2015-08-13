package fpinscala.parallelism

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{Callable,ExecutorService}
import annotation.tailrec

/*
 * Implementation is taken from `scalaz` library, with only minor changes. See:
 *
 * https://github.com/scalaz/scalaz/blob/scalaz-seven/concurrent/src/main/scala/scalaz/concurrent/Actor.scala
 *
 * This code is copyright Andriy Plokhotnyuk, Runar Bjarnason, and other contributors,
 * and is licensed using 3-clause BSD, see LICENSE file at:
 *
 * https://github.com/scalaz/scalaz/blob/scalaz-seven/etc/LICENCE
 */

/**
 * Processes messages of type `A`, one at a time. Messages are submitted to
 * the actor with the method `!`. Processing is typically performed asynchronously,
 * this is controlled by the provided `strategy`.
 *
 * Memory consistency guarantee: when each message is processed by the `handler`, any memory that it
 * mutates is guaranteed to be visible by the `handler` when it processes the next message, even if
 * the `strategy` runs the invocations of `handler` on separate threads. This is achieved because
 * the `Actor` reads a volatile memory location before entering its event loop, and writes to the same
 * location before suspending.
 *
 * Implementation based on non-intrusive MPSC node-based queue, described by Dmitriy Vyukov:
 * [[http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue]]
 *
 * @see scalaz.concurrent.Promise for a use case.
 *
 * @param handler  The message handler
 * @param onError  Exception handler, called if the message handler throws any `Throwable`.
 * @param strategy Execution strategy, for example, a strategy that is backed by an `ExecutorService`
 * @tparam A       The type of messages accepted by this actor.
 */

/*
  @https://github.com/scalaz/scalaz/blob/scalaz-seven/concurrent/src/main/scala/scalaz/concurrent/Actor.scala
    这段代码的实现的遍历的实现方式搞不懂, 感觉只能从head下遍历,而且只能是一个node。上边的nodes都遍历不到

  这个并发的结构是怎么工作的
 */
final class Actor[A](strategy: Strategy)(handler: A => Unit, onError: Throwable => Unit = throw(_)) {
  self => // :?: 这是啥意思

  private val tail = new AtomicReference(new Node[A]())
  private val suspended = new AtomicInteger(1)
  private val head = new AtomicReference(tail.get)

  /** Alias for `apply` */
  def !(a: A) {
    val n = new Node(a)
    head.getAndSet(n).lazySet(n)
    trySchedule()
  }

  /** Pass the message `a` to the mailbox of this actor */
  def apply(a: A) {
    this ! a
  }

  def contramap[B](f: B => A): Actor[B] =
    new Actor[B](strategy)((b: B) => (this ! f(b)), onError)

  private def trySchedule() {
    //compareAndSet(expected, update) 如果值和 expect 相等则更新 update 的值
    // 如果当前 suspended 中的值不为 1 则 schedule 不执行
    if (suspended.compareAndSet(1, 0)) schedule()
  }

  private def schedule() {
    strategy(act())
  }

  private def act() {
    val t = tail.get
    val n = batchHandle(t, 1024)//一次性处理1024个节点
    if (n ne t) {//还有剩余的节点数(总的节点数-处理完的节点数)
      n.a = null.asInstanceOf[A]
      //:?: head需要关联上把, 不关联上就无法继续添加任务了 - 这个没有必要关联这个位置将剩余的节点的最尾部重新设置到tail上了
      //所以head引用的位置并没有发生变化
      tail.lazySet(n)
      schedule()//这个位置呢个继续往下执行
    } else {//到达尾部（tail）引用节点
      suspended.set(1)// suspended == 1 && n.get == null 下面没有待处理的任务节点, 停止执行
      if (n.get ne null) trySchedule()//尾部还有未处理的节点, 继续执行
    }
  }

  //一次性处理i个节点任务
  @tailrec
  private def batchHandle(t: Node[A], i: Int): Node[A] = {
    val n = t.get
    // `ne` defined in `http://www.scala-lang.org/api/current/index.html#scala.AnyRef`
    // ne & eq 比较的是jvm的内存地址是否是相等的, 相当于 java 的 ==
    // 而 == 在scala中比较的是两个对象的值是否是相等的
    // `http://my.oschina.net/hanzhankang/blog/200295`
    if (n ne null) {
      try {
        handler(n.a)
      } catch {
        case ex: Throwable => onError(ex)//任何异常由onError处理
      }
      if (i > 0) batchHandle(n, i - 1) else n
    } else t
  }
}

// 这个 Node class 有一个type为A的成员变量, 并且集成自AtomicReference[Node[A]], 这意味着它具有.get():[Node[A]]的方法
// 这个地方.a只是用来暂存值
private class Node[A](var a: A = null.asInstanceOf[A]) extends AtomicReference[Node[A]]

object Actor {
  // 默认是用异步的Strategy来声明Actor
  /** Create an `Actor` backed by the given `ExecutorService`. */
  def apply[A](es: ExecutorService)(handler: A => Unit, onError: Throwable => Unit = throw(_)): Actor[A] =
    new Actor(Strategy.fromExecutorService(es))(handler, onError)
}

/**
 * Provides a function for evaluating expressions, possibly asynchronously.
 * The `apply` function should typically begin evaluating its argument
 * immediately. The returned thunk can be used to block until the resulting `A`
 * is available.
 */
trait Strategy {
  def apply[A](a: => A): () => A
}

object Strategy {

  /**
   * 异步步执行, 返回 Strategy 的 wrapper
   * We can create a `Strategy` from any `ExecutorService`. It's a little more
   * convenient than submitting `Callable` objects directly.
   */
  def fromExecutorService(es: ExecutorService): Strategy = new Strategy {
    def apply[A](a: => A): () => A = {
      val f = es.submit { new Callable[A] { def call = a} }
      () => f.get
    }
  }

  /**
   * 同步执行, 返回 Strategy 的 wrapper
   * A `Strategy` which begins executing its argument immediately in the calling thread.
   */
  def sequential: Strategy = new Strategy {
    def apply[A](a: => A): () => A = {
      val r = a
      () => r
    }
  }
}
