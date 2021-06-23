package com.udacity.project4.base

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.utils.EspressoIdlingResource

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()
        _viewModel.showErrorMessage.observe(this, Observer {
            val toast = Toast.makeText(activity, it, Toast.LENGTH_LONG)
/*            toast.addCallback(object : Toast.Callback() {
                override fun onToastShown() {
                    EspressoIdlingResource.increment()
                }

                override fun onToastHidden() {
                    EspressoIdlingResource.decrement()
                }
            })*/
            toast.show()
        })
        _viewModel.showToast.observe(this, Observer {
            val toast = Toast.makeText(activity, it, Toast.LENGTH_LONG)
/*            toast.addCallback(object : Toast.Callback() {
                override fun onToastShown() {
                    EspressoIdlingResource.increment()
                }

                override fun onToastHidden() {
                    EspressoIdlingResource.decrement()
                }
            })*/
            toast.show()
        })
        _viewModel.showSnackBar.observe(this, Observer {
            val snackBar = Snackbar.make(this.view!!, it, Snackbar.LENGTH_LONG)
            snackBar.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    EspressoIdlingResource.increment()
                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    EspressoIdlingResource.decrement()
                }
            })
            snackBar.show()
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            val snackBar = Snackbar.make(this.view!!, getString(it), Snackbar.LENGTH_LONG)
            snackBar.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    EspressoIdlingResource.increment()
                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    EspressoIdlingResource.decrement()
                }
            })
            snackBar.show()
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        })
    }
}