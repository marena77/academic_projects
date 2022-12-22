package com.example.collaborativeart

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.example.collaborativeart.R


class PermissionFragment : Fragment() {
    companion object {
        /** Required permission. */
        private const val PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    }
    private val gviewModel: GlobalViewModel by activityViewModels()
    /**
     * Registers a permission launcher callback that, when evoked,
     * will ask the user to grant the required permission and then
     * call the passed callback with the grant status. This launcher
     * can be launched by calling [ActivityResultLauncher.launch].
     */
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                // All permission are granted - navigate to next fragment.
                navigateToNextFragment()
            } else {
                // Permission denied - inform user that permission is needed.
                requireView().showSnackbar(
                    "We need your location permission to make a Post!",
                    Snackbar.LENGTH_INDEFINITE,
                    "Ok"
                ) {
                    // Use has clicked the OK button so ask for permission again.
                    relaunchPermissionRequest()
                }
            }
        }

    /**
     * Dynamically create a FrameLayout view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FrameLayout(requireContext()).also {
            it.id = View.generateViewId()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check for or ask for required permission.
        requestPermission()
    }

    /**
     * Check or get permission and load contacts once acquired.
     */
    private fun requestPermission() {
        // Check if any permissions are denied.
        val granted =
            ContextCompat.checkSelfPermission(requireContext(), PERMISSION) ==
                    PackageManager.PERMISSION_GRANTED

        when {
            // If no permissions have been denied, then navigate to the
            // next fragment.
            granted -> {
                navigateToNextFragment()
            }

            // Check if the user has previously denied permission and
            // therefore a rational should be provided to help the user
            // understand the importance of granting permission.
            shouldShowRequestPermissionRationale(PERMISSION) -> {
                requireView().showSnackbar(
                    "We need your location permission to make a Post!",
                    Snackbar.LENGTH_INDEFINITE,
                    "Ok"
                ) {
                    // Use has clicked the OK button so ask for permissions again.
                    requestPermissionLauncher.launch(PERMISSION)
                }
            }

            // User hasn't been asked for permission yet.
            else -> {
                // Start the permission request launcher.
                requestPermissionLauncher.launch(PERMISSION)
            }
        }
    }

    /**
     * Navigate to next fragment.
     */
    private fun navigateToNextFragment() {
        if (gviewModel.oldFragment.value == "Post") {
            gviewModel.navigateToPost(activity?.supportFragmentManager!!)
        } else {
            gviewModel.navigateToMap(activity?.supportFragmentManager!!)
        }
    }

    /**
     * Helper used by requestPermissionLauncher to relaunch the
     * permissions request. Note that if this method is called
     * more than once on API >= 29, the permissions framework
     * will not show the permissions dialog.
     */
    private fun relaunchPermissionRequest() {
        requestPermissionLauncher.launch(PERMISSION)
    }

}