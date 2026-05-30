package com.gazlaws.codeboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

// 인트로(튜토리얼) 슬라이드 한 장을 지정한 레이아웃으로 보여주는 프래그먼트
class IntroFragment : Fragment() {

    private var layoutResId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null && args.containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = args.getInt(ARG_LAYOUT_RES_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    companion object {
        private const val ARG_LAYOUT_RES_ID = "layoutResId"

        @JvmStatic
        fun newInstance(layoutResId: Int): IntroFragment {
            val sampleSlide = IntroFragment()
            val args = Bundle()
            args.putInt(ARG_LAYOUT_RES_ID, layoutResId)
            sampleSlide.arguments = args
            return sampleSlide
        }
    }
}
