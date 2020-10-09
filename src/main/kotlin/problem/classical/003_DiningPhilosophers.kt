package problem.classical

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock

private const val PHILOSOPHERS = 5
private const val REPEAT_TIMES = 10

class Table(val size: Int) {
    private val forks = Array(size) { ReentrantLock(true) }

    fun acquireFork(forkId: Int, timeout: Long, timeUnit: TimeUnit) {
        check(forkId in 0..forks.lastIndex)
        if (!forks[forkId].tryLock(timeout, timeUnit)) {
            throw TimeoutException()
        }
    }

    fun releaseFork(id: Int) {
        check(id in 0..forks.lastIndex)
        forks[id].unlock()
    }
}

abstract class Philosopher(private val repeat: Int, private val sitId: Int, protected val table: Table) {

    fun run() {
        repeat(repeat) {
            acquireForks()
            eat()
            releaseForks()
            think()
        }
    }

    protected abstract fun acquireForks()

    protected abstract fun releaseForks()

    protected fun leftForkId(): Int {
        return sitId
    }

    protected fun rightForkId(): Int {
        return if (sitId + 1 == table.size) 0 else sitId + 1
    }

    private fun eat() {
        println("Philosopher $sitId: eat")
        Thread.sleep(1000L)
    }

    private fun think() {
        println("Philosopher $sitId: think")
        Thread.sleep(1000L)
    }
}

class RightHandedPhilosopher(repeat: Int, sitId: Int, table: Table) : Philosopher(repeat, sitId, table) {
    override fun acquireForks() {
        table.acquireFork(rightForkId(), 10, TimeUnit.SECONDS)
        table.acquireFork(leftForkId(), 10, TimeUnit.SECONDS)
    }

    override fun releaseForks() {
        table.releaseFork(leftForkId())
        table.releaseFork(rightForkId())
    }
}

class LeftHandedPhilosopher(repeat: Int, sitId: Int, table: Table) : Philosopher(repeat, sitId, table) {
    override fun acquireForks() {
        table.acquireFork(leftForkId(), 10, TimeUnit.SECONDS)
        table.acquireFork(rightForkId(), 10, TimeUnit.SECONDS)
    }

    override fun releaseForks() {
        table.releaseFork(rightForkId())
        table.releaseFork(leftForkId())
    }
}

fun main() {
    val table = Table(PHILOSOPHERS)
    val threadPool = Executors.newFixedThreadPool(PHILOSOPHERS)
    (0 until PHILOSOPHERS)
            .map { i -> if (i % 2 == 0) {
                LeftHandedPhilosopher(REPEAT_TIMES, i, table)
            } else {
                RightHandedPhilosopher(REPEAT_TIMES, i, table)
            } }
            .map { p -> threadPool.submit { p.run() } }

    threadPool.shutdown()
    threadPool.awaitTermination(30, TimeUnit.SECONDS)
}