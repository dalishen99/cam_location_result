package com.es.multivs.presentation.view.fragments

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.constraintlayout.widget.Constraints
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.es.multivs.R
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.FragmentVideoVisitBinding
import com.es.multivs.presentation.view.viewmodels.VideoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class VideoVisitFragment: Fragment() {
    companion object {
        fun newInstance() = VideoVisitFragment()
    }
    private var binding: FragmentVideoVisitBinding by autoCleared()
    private val videoViewModel: VideoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoVisitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }


    private fun initData() {
         binding.btnClose.setOnClickListener {
             binding.webView.clearFormData()
             val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
             viewPager2.setCurrentItem(0, false)
          }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val data = videoViewModel.loadVideoUrl()
            binding.webView.settings.javaScriptEnabled = true
            binding.webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            binding.webView.settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
            binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
            binding.webView.settings.allowFileAccess = true
            binding.webView.webChromeClient = object : WebChromeClient() {
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
            binding.webView.loadUrl(data.video_Visit)
        }
    }
}