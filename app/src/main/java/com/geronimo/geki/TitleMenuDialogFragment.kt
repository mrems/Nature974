package com.geronimo.geki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction

class TitleMenuDialogFragment : DialogFragment() {

    interface TitleMenuListener {
        fun onMenuItemClick(itemId: Int)
    }

    var listener: TitleMenuListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_title_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.menu_preferences).setOnClickListener {
            listener?.onMenuItemClick(R.id.menu_preferences)
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
            dismiss()
        }
        view.findViewById<TextView>(R.id.menu_manual).setOnClickListener {
            listener?.onMenuItemClick(R.id.menu_manual)
            dismiss()
        }
        view.findViewById<TextView>(R.id.menu_feedback).setOnClickListener {
            listener?.onMenuItemClick(R.id.menu_feedback)
            dismiss()
        }
        view.findViewById<TextView>(R.id.menu_subscription).setOnClickListener {
            listener?.onMenuItemClick(R.id.menu_subscription)
            dismiss()
        }
    }

    override fun getTheme(): Int {
        return R.style.CustomTitleMenuDialog
    }
}
