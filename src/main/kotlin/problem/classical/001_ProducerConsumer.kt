package problem.classical

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*

const val QUEUE_CAPACITY = 10

class Producer(private val channel: Channel<Int>) {
    private val random: Random = Random()

    suspend fun produce() {
        for (i in 0..QUEUE_CAPACITY) {
            delay(random.nextInt(1000).toLong())
            channel.send(i)
            println("Producer: write $i")
        }
        channel.close()
    }
}

class Consumer(private val channel: Channel<Int>) {
    private val random: Random = Random()

    suspend fun consume() {
        for (v in channel) {
            println("Consumer: read $v")
            delay(random.nextInt(2000).toLong())
        }
    }
}

fun main() {
    runBlocking {
        val channel = Channel<Int>(QUEUE_CAPACITY)
        listOf(
            launch { Producer(channel).produce() },
            launch { Consumer(channel).consume() }
        ).joinAll()
    }
}