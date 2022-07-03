package com.mystravil.omniloader.ui.home

import android.Manifest
import android.R
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
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.mystravil.omniloader.databinding.FragmentHomeBinding
import com.yausername.youtubedl_android.DownloadProgressCallback
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File


class HomeFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val TAG = "FragmentHomeDownload"

    private var downloading = false
    private val compositeDisposable = CompositeDisposable()

    private val callback = DownloadProgressCallback { progress, etaInSeconds, line ->
        requireActivity().runOnUiThread(
            Runnable {
                _binding?.progressBar?.setProgress(progress.toInt())
                _binding?.tvStatus?.setText(line)
            }
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val targetDirPreference = prefs.getString("target_dir", "")

        Log.e(TAG, "targetDirPreference")
        if (targetDirPreference != null) {
            Log.e(TAG, targetDirPreference)
        }

        // _binding?.editTextLinkInput!!.setText("https://www.youtube.com/watch?v=BBJa32lCaaY")

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
        _binding?.editTextLinkInput?.isEnabled  = false
        _binding?.buttonDownload?.isEnabled  = false

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

        request.addOption("--no-check-certificate")
        request.addOption("--verbose")
        request.addOption("--no-mtime")

        if (_binding?.radioButtonFormatVideo?.isChecked == true) {
            Log.i(TAG, "Format: Video")
            request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")
            request.addOption("-f best")
        } else {
            Log.i(TAG, "Format: Audio")
            request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.mp3")
            request.addOption("-f ba")
//            request.addOption("-x")
//            request.addOption("--audio-format mp3")
        }

        showStart();

        val disposable: Disposable = Observable.fromCallable {
            YoutubeDL.getInstance().execute(request, callback)
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ youtubeDLResponse ->
                _binding?.pbStatus?.setVisibility(View.GONE)
                _binding?.progressBar?.setProgress(100)
                _binding?.tvStatus?.setText("Download complete!")
//                _binding?.tvCommandOutput?.setText(youtubeDLResponse.getOut())
                Toast.makeText(
                    requireActivity(),
                    "Download successful!",
                    Toast.LENGTH_LONG
                ).show()
                downloading = false

                _binding?.editTextLinkInput?.isFocusable  = true
                _binding?.editTextLinkInput?.isEnabled  = true
                _binding?.buttonDownload?.isFocusable  = true
                _binding?.buttonDownload?.isEnabled  = true
            }) { e ->
                Log.e(TAG, "Download failed!", e)
                _binding?.pbStatus?.setVisibility(View.GONE)
                _binding?.tvStatus?.setText("Download failed!")
//                _binding?.tvCommandOutput?.setText(e.message)
                Toast.makeText(
                    requireActivity(),
                    "Download failed!",
                    Toast.LENGTH_LONG
                ).show()
                downloading = false

                _binding?.editTextLinkInput?.isFocusable  = true
                _binding?.editTextLinkInput?.isEnabled  = true
                _binding?.buttonDownload?.isFocusable  = true
                _binding?.buttonDownload?.isEnabled  = true
            }
        compositeDisposable.add(disposable)
    }

    private fun showStart() {
        _binding?.tvStatus?.setText("Download started")
        _binding?.progressBar?.setProgress(0)
        _binding?.pbStatus?.setVisibility(View.VISIBLE)
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
        val youtubeDLDir = File(downloadsDir, "omniloader")
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir()
        return youtubeDLDir
    }
}