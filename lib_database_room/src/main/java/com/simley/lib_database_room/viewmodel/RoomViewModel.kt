import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.simley.lib_database_room.db.model.User
import com.simley.lib_database_room.manager.RoomManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomViewModel(val context: Application) : AndroidViewModel(context) {
    var users = MutableLiveData<List<User>>()
    fun queryUsers() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                RoomManager.getDB(context).userDao().getAll()
            }
            users.value = list
        }
    }

    fun insertUser(user: User) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                RoomManager.getDB(context).userDao().insertAll(user)
            }
        }
    }
}
