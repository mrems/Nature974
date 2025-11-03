package com.pastaga.geronimo

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment : Fragment(), GalleryAdapter.OnImageClickListener {

	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: GalleryAdapter

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_gallery, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		recyclerView = view.findViewById(R.id.gallery_recycler)
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
		recyclerView.setHasFixedSize(true)
		recyclerView.itemAnimator = null
		adapter = GalleryAdapter(this)
		recyclerView.adapter = adapter

		viewLifecycleOwner.lifecycleScope.launch {
			val images = withContext(Dispatchers.IO) { loadImages() }
			adapter.submitList(images)
		}
	}

	private fun loadImages(): List<Uri> {
		val imageUris = mutableListOf<Uri>()
		val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		} else {
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI
		}
		val projection = arrayOf(MediaStore.Images.Media._ID)
		val sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"
		requireContext().contentResolver.query(
			collection,
			projection,
			null,
			null,
			sortOrder
		)?.use { cursor ->
			val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
			var count = 0
			while (cursor.moveToNext()) {
				val id = cursor.getLong(idColumn)
				imageUris.add(ContentUris.withAppendedId(collection, id))
				count++
				if (count >= 300) break
			}
		}
		return imageUris
	}

	override fun onImageClicked(uri: Uri) {
		parentFragmentManager.setFragmentResult("gallery_result", bundleOf("uri" to uri.toString()))
	}
}




