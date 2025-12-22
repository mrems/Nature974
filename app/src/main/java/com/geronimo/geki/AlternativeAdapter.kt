package com.geronimo.geki

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color // Ajout de l'import pour Color

class AlternativeAdapter(private val alternatives: List<AlternativeIdentification>, private val confidenceScore: Int?) :
    RecyclerView.Adapter<AlternativeAdapter.AlternativeViewHolder>() {

    // Couleurs du dÃ©gradÃ© pour le trait (doivent correspondre Ã  custom_progress_bar.xml)
    private val startColor = Color.parseColor("#8BC34A") // Vert clair
    private val endColor = Color.parseColor("#2196F3") // Bleu clair

    class AlternativeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scientificName: TextView = itemView.findViewById(R.id.alternative_scientific_name)
        val localName: TextView = itemView.findViewById(R.id.alternative_local_name)
        val difference: TextView = itemView.findViewById(R.id.alternative_difference)
        val verticalBar: View = itemView.findViewById(R.id.alternative_vertical_bar) // Ajout de la rÃ©fÃ©rence au trait vertical
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

        // Appliquer la couleur interpolÃ©e au trait vertical si le score de confiance est disponible
        confidenceScore?.let { score ->
            val interpolatedColor = interpolateColor(score)
            holder.verticalBar.setBackgroundColor(interpolatedColor)
        }
    }

    /**
     * Interpole une couleur entre la couleur de dÃ©but et la couleur de fin en fonction d'une progression (0-100).
     */
    private fun interpolateColor(progress: Int): Int {
        val fraction = progress / 100f
        val inverseFraction = 1 - fraction

        val a = (Color.alpha(startColor) * inverseFraction + Color.alpha(endColor) * fraction).toInt()
        val r = (Color.red(startColor) * inverseFraction + Color.red(endColor) * fraction).toInt()
        val g = (Color.green(startColor) * inverseFraction + Color.green(endColor) * fraction).toInt()
        val b = (Color.blue(startColor) * inverseFraction + Color.blue(endColor) * fraction).toInt()

        return Color.argb(a, r, g, b)
    }

    override fun getItemCount(): Int = alternatives.size
}

