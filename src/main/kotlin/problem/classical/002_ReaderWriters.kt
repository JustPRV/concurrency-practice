package problem.classical

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantReadWriteLock

const val REPEAT = 10
const val READERS = 5
const val WRITERS = 2

class ValueHolder {
    private val valueLock = ReentrantReadWriteLock()
    private var value: String = "empty"

    fun getValue(timeout: Long, unit: TimeUnit): String {
        val rl = valueLock.readLock()
        try {
            if (rl.tryLock(timeout, unit)) {
                return value
            }
            throw TimeoutException()
        } finally {
            rl.unlock()
        }
    }

    fun setValue(value: String, timeout: Long, unit: TimeUnit) {
        val wl = valueLock.writeLock()
        try {
            if (wl.tryLock(timeout, unit)) {
                this.value = value
                return
            }
            throw TimeoutException()
        } finally {
            wl.unlock()
        }
    }
}

class Reader(private val id: Int, private val valueHolder: ValueHolder) {
    private val random = Random()

    fun read() {
        for (i in 0..REPEAT) {
            val value = valueHolder.getValue((WRITERS + 1).toLong(), TimeUnit.SECONDS)
            println("Reader $id: $value")
            Thread.sleep(random.nextInt(1000).toLong())
        }
    }
}

class Writer(private val id: Int, private val valueHolder: ValueHolder) {
    private val random = Random()

    fun write() {
        for (i in 0..REPEAT) {
            val v = "$id-$i"
            valueHolder.setValue(v, (READERS + 1).toLong(), TimeUnit.SECONDS)
            println("Writer $id: $v")
            Thread.sleep(random.nextInt(1000).toLong())
        }
    }
}

fun main() {
    val threadPool = Executors.newFixedThreadPool(READERS + WRITERS)
    val valueHolder = ValueHolder()
    (0..READERS).map { i -> Reader(i, valueHolder) }.map { r -> threadPool.submit { r.read() } }
    (0..WRITERS).map { i -> Writer(i, valueHolder) }.map { w -> threadPool.submit { w.write() } }

    threadPool.shutdown()
    threadPool.awaitTermination(30, TimeUnit.SECONDS)
}