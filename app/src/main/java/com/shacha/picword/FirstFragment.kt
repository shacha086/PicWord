package com.shacha.picword

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.isPhotoPickerAvailable
import androidx.core.content.ContextCompat
import androidx.core.view.allViews
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import com.shacha.picword.databinding.DocumentCardBinding
import com.shacha.picword.databinding.FragmentFirstBinding
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FirstFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FirstFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentFirstBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        // Inflate the layout for this fragment
        binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        if (isPhotoPickerAvailable(context) || ContextCompat.checkSelfPermission(
                context,
                READ_EXTERNAL_STORAGE
            ) == PERMISSION_GRANTED
        ) {
            // if at least Android 13, using photo picker api instead of grant permission
            binding.grantPermissionCard.visibility = View.GONE
        }
        (context.filesDir.listFiles { file ->
            file.extension == "docx"
        }?.sortedByDescending { it.lastModified() } ?: emptyList<File>()).forEach {
            addDocumentCard(inflater, it)
        }
        return binding.root
    }

    fun addDocumentCard(inflater: LayoutInflater, file: File, index: Int = -1) {
        val cardBinding = DocumentCardBinding.inflate(inflater, binding.linearLayout, false)
        cardBinding.lifecycleOwner = this
        cardBinding.holder = DocumentCardDataHolder(
            ObservableField(file.nameWithoutExtension),
            ObservableField(file)
        )
        cardBinding.listener = DocumentCardClickListenerImpl
        binding.linearLayout.addView(cardBinding.root, index)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FirstFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FirstFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}