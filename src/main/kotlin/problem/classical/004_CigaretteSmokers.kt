package problem.classical

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val REPEAT_TIMES = 10

class Agent {
    val agent = Semaphore(1)
    val tobacco = Semaphore(REPEAT_TIMES, REPEAT_TIMES)
    val paper = Semaphore(REPEAT_TIMES, REPEAT_TIMES)
    val match = Semaphore(REPEAT_TIMES, REPEAT_TIMES)

    val tobaccoPaper = Semaphore(REPEAT_TIMES, REPEAT_TIMES)
    val tobaccoMatch = Semaphore(REPEAT_TIMES, REPEAT_TIMES)
    val paperMatch = Semaphore(REPEAT_TIMES, REPEAT_TIMES)

    val mutex = Semaphore(1)
    var tobaccoNum = 0
    var paperNum = 0
    var matchNum = 0
}

class AgentA(private val agent: Agent) {
    suspend fun run() {
        repeat(REPEAT_TIMES) {
            agent.agent.acquire()
            agent.tobacco.release()
            agent.paper.release()
        }
    }
}

class AgentB(private val agent: Agent) {
    suspend fun run() {
        repeat(REPEAT_TIMES) {
            agent.agent.acquire()
            agent.paper.release()
            agent.match.release()
        }
    }
}

class AgentC(private val agent: Agent) {
    suspend fun run() {
        repeat(REPEAT_TIMES) {
            agent.agent.acquire()
            agent.tobacco.release()
            agent.match.release()
        }
    }
}

class PusherA(private val agent: Agent) {
    suspend fun run() {
        while(true) {
            agent.tobacco.acquire()
            agent.mutex.withPermit {
                when {
                    agent.paperNum > 0 -> {
                        agent.paperNum--
                        agent.tobaccoPaper.release()
                    }
                    agent.matchNum > 0 -> {
                        agent.matchNum--
                        agent.tobaccoMatch.release()
                    }
                    else -> {
                        agent.tobaccoNum++
                    }
                }
            }
        }
    }
}

class PusherB(private val agent: Agent) {
    suspend fun run() {
        while(true) {
            agent.paper.acquire()
            agent.mutex.withPermit {
                when {
                    agent.tobaccoNum > 0 -> {
                        agent.tobaccoNum--
                        agent.tobaccoPaper.release()
                    }
                    agent.matchNum > 0 -> {
                        agent.matchNum--
                        agent.paperMatch.release()
                    }
                    else -> {
                        agent.paperNum++
                    }
                }
            }
        }
    }
}

class PusherC(private val agent: Agent) {
    suspend fun run() {
        while(true) {
            agent.match.acquire()
            agent.mutex.withPermit {
                when {
                    agent.tobaccoNum > 0 -> {
                        agent.tobaccoNum--
                        agent.tobaccoMatch.release()
                    }
                    agent.paperNum > 0 -> {
                        agent.paperNum--
                        agent.paperMatch.release()
                    }
                    else -> {
                        agent.matchNum++
                    }
                }
            }
        }
    }
}

class SmokerA(private val agent: Agent) {
    suspend fun run() {
        repeat(REPEAT_TIMES) {
            agent.tobaccoPaper.acquire()
            println("A done")
            agent.agent.release()
        }
    }
}

class SmokerB(private val agent: Agent) {
    suspend fun run() {
        repeat(REPEAT_TIMES) {
            agent.paperMatch.acquire()
            println("B done")
            agent.agent.release()
        }
    }
}

class SmokerC(private val agent: Agent) {
    suspend fun run() {
        repeat(REPEAT_TIMES) {
            agent.tobaccoMatch.acquire()
            println("C done")
            agent.agent.release()
        }
    }
}

fun main() {
    val agent = Agent()
    runBlocking {
        launch { AgentA(agent).run() }
        launch { AgentB(agent).run() }
        launch { AgentC(agent).run() }
        val pushers = listOf(
                launch { PusherA(agent).run() },
                launch { PusherB(agent).run() },
                launch { PusherC(agent).run() }
        )
        val smokers = listOf(
                launch { SmokerA(agent).run() },
                launch { SmokerB(agent).run() },
                launch { SmokerC(agent).run() }
        )
        smokers.joinAll()
        pushers.forEach { j -> j.cancel() }
    }
}