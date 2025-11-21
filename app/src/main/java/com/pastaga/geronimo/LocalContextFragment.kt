package com.pastaga.geronimo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pastaga.geronimo.R
import android.widget.ImageView

class LocalContextFragment : Fragment() {

    private var localContext: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            localContext = it.getString(ARG_LOCAL_CONTEXT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_local_context, container, false)

        val localContextCard = view.findViewById<View>(R.id.info_card_local_context)
        val localContextTitle = localContextCard.findViewById<TextView>(R.id.title_info)
        val localContextContent = localContextCard.findViewById<TextView>(R.id.content_info)
        val localContextIcon = localContextCard.findViewById<ImageView>(R.id.icon_info)

        localContextTitle.text = "Contexte Local"
        localContextContent.text = localContext
        localContextIcon.setImageResource(R.drawable.local)
        localContextIcon.visibility = if (localContext != null && localContext != "N/C") View.VISIBLE else View.GONE
        localContextCard.visibility = if (localContext != null && localContext != "N/C") View.VISIBLE else View.GONE

        return view
    }

    companion object {
        private const val ARG_LOCAL_CONTEXT = "local_context"

        @JvmStatic
        fun newInstance(localContext: String) = LocalContextFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_LOCAL_CONTEXT, localContext)
            }
        }
    }
}
