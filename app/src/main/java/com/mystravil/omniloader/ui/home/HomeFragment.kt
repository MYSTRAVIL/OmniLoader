package com.mystravil.omniloader.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mystravil.omniloader.databinding.FragmentHomeBinding
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var downloading = false

    private val TAG = "FragmentHomeDownload"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _binding?.buttonDownload!!.setOnClickListener {
            this.startDownload()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startDownload() {
        if (downloading) {
            Toast.makeText(
                activity,
                "A download is already in progress",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!permissionsGranted()) {
            Toast.makeText(
                activity,
                "Grant permissions and retry",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val url = _binding?.editTextLinkInput!!.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(url)) {
            _binding?.editTextLinkInput!!.error = "URL input is empty!"
            return
        }

        val youtubeDLDir = getDownloadLocation()
        val request = YoutubeDLRequest(url)
        request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")
        request.addOption("--no-check-certificate")
        request.addOption("--verbose")
        request.addOption("-f best")
        downloading = true
        YoutubeDL.getInstance().execute(request)
        downloading = false
    }

    private fun permissionsGranted(): Boolean {
        val listPermissionsNeeded = mutableListOf<String>()

        val storage =
            ContextCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        val internet =
            ContextCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.INTERNET
            )

        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (internet != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.INTERNET)
        }

        Log.i(TAG, listPermissionsNeeded.toString())

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                listPermissionsNeeded.toTypedArray(),
                1
            )
            return false
        }
        return true
    }

    private fun getDownloadLocation(): File {
        val downloadsDir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "youtubedl-android")
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir()
        return youtubeDLDir
    }
}