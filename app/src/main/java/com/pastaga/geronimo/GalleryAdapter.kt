package com.pastaga.geronimo

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load

class GalleryAdapter(private val listener: OnImageClickListener) :
	ListAdapter<Uri, GalleryAdapter.ImageViewHolder>(DiffCallback) {

	interface OnImageClickListener {
		fun onImageClicked(uri: Uri)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_gallery_image, parent, false)
		return ImageViewHolder(view)
	}

	override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val imageView: ImageView = itemView.findViewById(R.id.gallery_item_image)
		fun bind(uri: Uri) {
			imageView.load(uri) {
				crossfade(true)
			}
			itemView.setOnClickListener { listener.onImageClicked(uri) }
		}
	}

	private object DiffCallback : DiffUtil.ItemCallback<Uri>() {
		override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
		override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean = oldItem == newItem
	}
}




