package dubrowgn.wattz

fun Double?.div(v: Double?) : Double? {
    if (v == null)
        return null

    return this?.div(v)
}

fun Double?.times(v: Double?) : Double? {
    if (v == null)
        return null

    return this?.times(v)
}
