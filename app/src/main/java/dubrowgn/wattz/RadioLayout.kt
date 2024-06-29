package dubrowgn.wattz

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.RadioButton
import androidx.constraintlayout.widget.ConstraintLayout

typealias checkChangedHandler = (id: Int) -> Unit

class RadioLayout : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var checkedId: Int = -1
    var checkChangedCallback: checkChangedHandler? = null
    private val radios = ArrayList<RadioButton>()
    private var updateInProgress = false

    fun check(id: Int) {
        activateRadio(findViewById(id))
    }

    val checkedRadioButtonId : Int get() = checkedId

    private fun activateRadio(radio: RadioButton) {
        if (updateInProgress || checkedId == radio.id)
            return

        // suppress recursive updates
        updateInProgress = true
        checkedId = radio.id
        for (r in radios) {
            r.isChecked = false
        }
        radio.isChecked = true
        updateInProgress = false

        checkChangedCallback?.invoke(checkedId)
    }

    override fun onViewAdded(view: View?) {
        super.onViewAdded(view)

        val rb = view as? RadioButton
        if (rb != null) {
            radios.add(rb)
            rb.setOnCheckedChangeListener { _, _ -> activateRadio(rb) }
        }
    }
}
