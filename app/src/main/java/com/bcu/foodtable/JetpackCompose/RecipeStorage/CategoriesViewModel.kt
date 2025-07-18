import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CategoriesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _categoryRecipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val categoryRecipes = _categoryRecipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadCategoryRecipes(categoryName: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val snapshot = db.collection("recipe")
                .whereArrayContains("c_categories", categoryName)
                .get()
                .await()

            if (isActive) {
                _categoryRecipes.value = snapshot.documents.mapNotNull {
                    it.toObject(RecipeItem::class.java)?.apply { id = it.id }
                }
            }
            _isLoading.value = false
        }
    }
}
