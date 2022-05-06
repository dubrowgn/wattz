package dubrowgn.wattz

import kotlin.math.absoluteValue

fun fmt(v: Double?): String {
    if (v == null)
        return "- "

    if (v.absoluteValue >= 10.0)
        return "%.0f".format(v)

    return "%.1f".format(v)
}
