import kotlinx.coroutines.*

/**
 * @author    yiliyang
 * @date      2021/8/11 上午8:47
 * @version   1.0
 * @since     1.0
 */
val handler = CoroutineExceptionHandler { _, throwable ->
    println("handler catch ${throwable.message}")
}

val handler2 = CoroutineExceptionHandler { _, throwable ->
    println("handler2 catch ${throwable.message}")
}


fun main() = runBlocking {
//    testException1()
//    testException2()
//    testException3()
//    testException4()
    testException5()
    return@runBlocking
}

/**
 * coroutineScope和SupervisorJob机制差不多,
 * 但是handler只能在上一级(加了launch的情况,如果没加,在supervisorScope下面是可以用handler的)处理,
 * 因为supervisorScope不能加handler
 */
suspend fun CoroutineScope.testException5() {
    supervisorScope {
        launch {
            repeat(100) {
                delay(1000)
                println("repeat in coroutineScope outer")
            }
        }
//        launch() {
//            launch {
//                repeat(100) {
//                    delay(1000)
//                    println("repeat in coroutineScope inner")
//                }
//            }
//            launch(handler) {
//                delay(3000)
//                throw RuntimeException("exception cancel coroutineScope")
//            }
//        }
        launch(handler) {
            delay(3000)
            throw RuntimeException("exception cancel coroutineScope")
        }
    }
}

/**
 * SupervisorJob() 中的每个协程必须单独launch,或者使用with,因为当出现异常,异常要传播到到CoroutineScope
 * 但是不会cancelparent,所以只会cancel掉出现异常的那个分支,所以inner会被cancel,但是outer不会
 */
suspend fun CoroutineScope.testException4() {
    with(CoroutineScope(SupervisorJob() + handler)) {
        launch {
            repeat(100) {
                delay(1000)
                println("repeat in SupervisorJob outer")
            }
        }
        launch {
            launch {
                repeat(100) {
                    delay(1000)
                    println("repeat in SupervisorJob inner")
                }
            }
            launch {
                delay(3000)
                throw RuntimeException("exception cancel SupervisorJob")
            }
        }
    }
    delay(10000)
}

/**
 * job 如果被cancel,会抛出JobCancellationException,这个异常会被handler忽略
 *
 * 如果通过异常导致job取消,会抛出JobCancellationException,抛出的异常会被handler捕获
 */
suspend fun CoroutineScope.testException3() {
    val job = CoroutineScope(handler).launch {
        val job = launch {
            try {
                delay(Long.MAX_VALUE)
            } catch (e: Exception) {
                println(e)
            } finally {
                withContext(NonCancellable) {
                    //就算被cancel掉了,还能够使用协程执行操作s
                }
            }
        }
        delay(1000)
        println("cancel job")
//        job.cancel()
        throw RuntimeException("cancel with exception")
        println("job canceled")
    }
    job.join()
}

/**
 * async的异常不用wait也可以捕获
 *
 * async的异常加上try catch会捕获两次
 */
suspend fun CoroutineScope.testException2() {
//    val job = CoroutineScope(handler).launch {
//        val asyncJob = async {
//            delay(1000)
//            throw RuntimeException("async throw exception")
//        }
//    }
//    job.join()

    val job = CoroutineScope(handler).launch {
        val asyncJob = async<Unit> {
            delay(1000)
            throw RuntimeException("async throw exception")
        }
        try {
            asyncJob.await()
        } catch (e: Exception) {
            println(e)
        }
    }
    job.join()
}

/**
 * 测试异常范围
 *
 * handler必须用到CoroutineScope上不然没效果
 */
suspend fun CoroutineScope.testException1() {
//    launch(handler) {
//        launch {
//            repeat(10) {
//                println("in repeat")
//                delay(1000)
//            }
//        }
////job2
//        launch {
//            launch {
//                delay(2000)
//                throw RuntimeException("throw RuntimeException")
//            }
//            println("out of throw exception")
//        }
//    }

//    ================================
    val job = CoroutineScope(handler).launch {
        val job2 = CoroutineScope(handler2).launch {
            launch {
                launch {
                    repeat(10) {
                        println("in repeat")
                        delay(1000)
                    }
                }
//job2
                launch {
                    launch {
                        delay(2000)
                        throw RuntimeException("throw RuntimeException")
                    }
                    println("out of throw exception")
                }
            }
        }
        job2.join()
    }
    job.join()


}
