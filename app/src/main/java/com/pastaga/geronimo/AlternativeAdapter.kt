package com.pastaga.geronimo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlternativeAdapter(private val alternatives: List<AlternativeIdentification>) :
    RecyclerView.Adapter<AlternativeAdapter.AlternativeViewHolder>() {

    class AlternativeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scientificName: TextView = itemView.findViewById(R.id.alternative_scientific_name)
        val localName: TextView = itemView.findViewById(R.id.alternative_local_name)
        val difference: TextView = itemView.findViewById(R.id.alternative_difference)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlternativeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alternative_identification, parent, false)
        return AlternativeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlternativeViewHolder, position: Int) {
        val alternative = alternatives[position]
        holder.scientificName.text = alternative.scientificName
        holder.localName.text = alternative.localName ?: "N/C"
        holder.difference.text = alternative.difference
    }

    override fun getItemCount(): Int = alternatives.size
}

