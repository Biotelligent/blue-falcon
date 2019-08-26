package dev.bluefalcon.adapters

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.RequiresApi
import dev.bluefalcon.BluetoothPeripheral
import dev.bluefalcon.viewModels.DevicesViewModel
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.padding
import org.jetbrains.anko.textView

class DevicesAdapter(viewModel : DevicesViewModel) : BaseAdapter() {
    private var list : List<BluetoothPeripheral> = viewModel.devices

    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    override fun getView(i : Int, v : View?, parent : ViewGroup?) : View {
        val item = getItem(i)
        return with(parent!!.context) {
            linearLayout {
                textView(item.bluetoothDevice.address) {
                    textSize = 20f
                    padding = 10
                }
                textView(item.bluetoothDevice.name) {
                    textSize = 12f
                    padding = 10
                }
            }
        }
    }

    override fun getItem(position : Int) : BluetoothPeripheral = list[position]

    override fun getCount() : Int = list.size

    override fun getItemId(position : Int) : Long = position.toLong()

}