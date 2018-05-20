package com.grarak.ytfetcher.views.recyclerview

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView

import com.grarak.ytfetcher.R
import com.grarak.ytfetcher.utils.server.user.User

class UserItem(private val isAdmin: Boolean,
               private val user: User,
               private val userListener: UserListener) : RecyclerViewItem() {
    private var verifiedSwitch: SwitchCompat? = null

    override val layoutXml = R.layout.item_user

    private val verifiedSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        userListener.onVerified(this@UserItem, isChecked)
    }

    interface UserListener {
        fun onClick(item: UserItem)

        fun onVerified(item: UserItem, verified: Boolean)
    }

    override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val title = viewHolder.itemView.findViewById<TextView>(R.id.title)
        val admin = viewHolder.itemView.findViewById<View>(R.id.admin_view)
        verifiedSwitch = viewHolder.itemView.findViewById(R.id.verified)

        title.text = user.name
        admin.visibility = if (user.admin) View.VISIBLE else View.GONE
        verifiedSwitch!!.isChecked = user.verified

        viewHolder.itemView.setOnClickListener { userListener.onClick(this) }

        if (isAdmin && !user.admin) {
            verifiedSwitch!!.visibility = View.VISIBLE
            verifiedSwitch!!.setOnCheckedChangeListener(verifiedSwitchListener)
        } else {
            verifiedSwitch!!.visibility = View.INVISIBLE
        }
    }

    fun setVerified(verified: Boolean) {
        verifiedSwitch!!.setOnCheckedChangeListener(null)
        verifiedSwitch!!.isChecked = verified
        verifiedSwitch!!.setOnCheckedChangeListener(verifiedSwitchListener)
    }
}
