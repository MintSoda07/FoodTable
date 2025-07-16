import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CategoriesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val categoryRecipes = MutableStateFlow<List<RecipeItem>>(emptyList())

    fun loadCategoryRecipes(categoryName: String) {
        viewModelScope.launch {
            val snapshot = db.collection("recipe")
                .whereArrayContains("c_categories", categoryName)
                .get()
                .await()

            categoryRecipes.value = snapshot.documents.mapNotNull {
                it.toObject(RecipeItem::class.java)?.apply { id = it.id }
            }
        }
    }
}
