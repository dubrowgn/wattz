package dubrowgn.wattz

import android.util.Log

fun trace() {
    val caller = Thread.currentThread().stackTrace[3]
    Log.d(caller.className, caller.methodName + "()")
}

fun info(msg: String) {
    val caller = Thread.currentThread().stackTrace[3]
    Log.i(caller.className, msg)
}

fun error(msg: String) {
    val caller = Thread.currentThread().stackTrace[3]
    Log.e(caller.className, msg)
}
