package com.zlove.day.up.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zlove.day.up.R
import kotlinx.android.synthetic.main.dialog_view_bottom.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2020/12/17.
 * PS: Not easy to write code, please indicate.
 */
class BottomDialogFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(): BottomDialogFragment {
            return BottomDialogFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.dialog_view_bottom, container, false)
    }

    fun setText(content: String) {
        text.text = content
    }

}