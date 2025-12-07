package com.pastaga.geronimo

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.load

class HistoryAdapter(
    private val historyList: MutableList<AnalysisEntry>,
    private val onClick: (AnalysisEntry) -> Unit,
    private val onOptionsClick: (AnalysisEntry, View) -> Unit
) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    init {
        setHasStableIds(true)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.history_item_image)
        val localNameTextView: TextView = itemView.findViewById(R.id.history_item_local_name)
        val scientificNameTextView: TextView = itemView.findViewById(R.id.history_item_scientific_name)
        val timestampTextView: TextView = itemView.findViewById(R.id.history_item_timestamp)

        fun bind(entry: AnalysisEntry) {
            imageView.load(Uri.parse(entry.imageUri))
            localNameTextView.text = entry.localName
            scientificNameTextView.text = entry.scientificName
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            timestampTextView.text = entry.timestamp?.let { sdf.format(Date(it)) } ?: "N/A"
            
            itemView.setOnClickListener { onClick(entry) }
            itemView.setOnLongClickListener { 
                onOptionsClick(entry, it)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_analysis_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    override fun getItemId(position: Int): Long {
        val entry = historyList[position]
        // Utiliser l'URI d'image comme ID stable (hash converti en Long)
        return entry.imageUri.hashCode().toLong()
    }

    fun updateItems(newItems: List<AnalysisEntry>) {
        historyList.clear()
        historyList.addAll(newItems)
        notifyDataSetChanged()
    }
}


