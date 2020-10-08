package problem.classical

import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

const val REPEAT = 10
const val READERS = 5
const val WRITERS = 2

class ValueHolder {
    private val valueLock = ReentrantReadWriteLock()
    var value: String = "empty"
        get() = valueLock.read { field }
        set(value) = valueLock.write { field = value }
}

class Reader(private val id: Int, private val valueHolder: ValueHolder) {
    private val random = Random()

    suspend fun read() {
        for (i in 0..REPEAT) {
            val value = valueHolder.value
            println("Reader $id: $value")
            delay(random.nextInt(1000).toLong())
        }
    }
}

class Writer(private val id: Int, private val valueHolder: ValueHolder) {
    private val random = Random()

    suspend fun write() {
        for (i in 0..REPEAT) {
            val v = "$id-$i"
            valueHolder.value = v
            println("Writer $id: $v")
            delay(random.nextInt(1000).toLong())
        }
    }
}

fun main() {
    runBlocking {
        val valueHolder = ValueHolder()
        listOf(
            (0..READERS).map { i -> Reader(i, valueHolder) }.map { r -> launch { r.read() } },
            (0..WRITERS).map { i -> Writer(i, valueHolder) }.map { w -> launch { w.write() } }
        ).flatten().joinAll()
    }
}