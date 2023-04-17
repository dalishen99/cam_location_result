package com.es.multivs.presentation.view.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import androidx.constraintlayout.widget.Constraints
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.R
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.FragmentVideoVisitBinding
import com.es.multivs.presentation.view.viewmodels.VideoViewModel
import kotlinx.coroutines.launch


/**
 * created by Dinesh
 * Etrog Systems LTD. 22/4/2022.
 */
@AndroidEntryPoint
class VideoAlertDialogFragment : DialogFragment() {

    private var binding: FragmentVideoVisitBinding by autoCleared()
    private val videoViewModel: VideoViewModel by activityViewModels()

    companion object {
        fun newInstance(): VideoAlertDialogFragment {
            return VideoAlertDialogFragment()
        }
    }

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoVisitBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            val data = videoViewModel.loadVideoUrl()
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            binding.webView.settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
            binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
            binding.webView.settings.allowFileAccess = true
            binding.webView.webChromeClient = object : WebChromeClient() {
                // Grant permissions for cam
                override fun onPermissionRequest(request: PermissionRequest) {
                    Log.d(Constraints.TAG, "onPermissionRequest")
                    val activity = activity
                    if (activity == null) {
                        Log.d(Constraints.TAG, "onPermissionRequest: getActivity was null")
                        return
                    }
                    activity.runOnUiThread(Runnable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (request.origin.toString()
                                    .contains("medpod")
                            ) request.grant(request.resources) else request.deny()
                        }
                    })
                }

                override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                    return true
                }
            }
            binding.webView.loadUrl(data.care_Coordinator)
        }

        binding.btnClose.setOnClickListener {
            binding.webView.destroy()
            dismiss()
        }
    }
}