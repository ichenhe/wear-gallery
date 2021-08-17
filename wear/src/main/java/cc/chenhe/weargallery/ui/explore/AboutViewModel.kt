package cc.chenhe.weargallery.ui.explore

import android.app.Application
import androidx.lifecycle.*
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.GITHUB
import cc.chenhe.weargallery.common.util.NetUtil
import cc.chenhe.weargallery.common.util.getVersionCode
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch

class AboutViewModel(application: Application, private val moshi: Moshi) :
    AndroidViewModel(application) {

    // 0: checking
    // 1: find new version
    // 2: no new version
    // 3: failed to check
    private val _newVersion = MutableLiveData(2)
    val newVersion: LiveData<Int> = _newVersion

    var url = GITHUB

    val checkUpdateBtnTitle = newVersion.map {
        when (it) {
            1 -> application.getString(R.string.about_find_new_version)
            2 -> application.getString(R.string.about_no_new_version)
            3 -> application.getString(R.string.about_recheck_new_version)
            else -> ""
        }
    }

    fun checkUpdate() {
        if (_newVersion.value!! == 0) {
            return
        }
        _newVersion.value = 0
        viewModelScope.launch {
            val resp = NetUtil.checkUpdate(moshi)
            if (resp == null) {
                _newVersion.postValue(3)
                return@launch
            }
            if (resp.wear?.latest?.code ?: 0 > getVersionCode(getApplication())) {
                url = resp.wear?.url ?: GITHUB
                _newVersion.postValue(1)
            } else {
                _newVersion.postValue(2)
            }
        }
    }

}