package com.bcu.foodtable.ui.subscribeNavMenu


import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.IngredientAdapter
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.*
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

class EditRecipeActivity : AppCompatActivity() {

    private val TAG = "EditRecipeActivity"

    private lateinit var titleEdit: EditText
    private lateinit var descEdit: EditText
    private lateinit var noteEdit: EditText
    private lateinit var imageView: ImageView
    private lateinit var imageBtn: Button
    private lateinit var uploadBtn: Button
    private lateinit var categorySpinner1: Spinner
    private lateinit var categorySpinner2: Spinner

    private lateinit var ingreRecycler: RecyclerView
    private lateinit var tagRecycler: RecyclerView
    private lateinit var stageRecycler: RecyclerView

    private lateinit var ingreField: EditText
    private lateinit var ingreAddBtn: Button
    private lateinit var tagField: EditText
    private lateinit var tagAddBtn: Button

    private lateinit var stageTitle: EditText
    private lateinit var stageDesc: EditText
    private lateinit var stageAddBtn: Button
    private lateinit var timerSwitch: Switch
    private lateinit var timerLayout: LinearLayout
    private lateinit var timerHour: EditText
    private lateinit var timerMin: EditText
    private lateinit var timerSec: EditText
    private lateinit var methodEdit: EditText

    private val db = FirebaseFirestore.getInstance()
    private var recipeId: String = ""
    private var imageUri: Uri? = null
    private var originalImageUrl: String = ""

    private val ingredients = mutableListOf<String>()
    private val tags = mutableListOf<String>()
    private val steps = mutableListOf<String>()

    private lateinit var ingreAdapter: IngredientAdapter
    private lateinit var tagAdapter: FlexAdaptor
    private lateinit var stepAdapter: RecipeDetailRecyclerAdaptor

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            Glide.with(this).load(it).into(imageView)
            Log.d(TAG, "이미지 선택됨: $uri")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_recipe)

        recipeId = intent.getStringExtra("recipe_id") ?: ""
        Log.d(TAG, "받은 recipe_id: $recipeId")

        bindViews()
        setupSpinners()
        setupAdapters()
        setupListeners()

        if (recipeId.isNotEmpty()) {
            loadRecipe()
        } else {
            Log.e(TAG, "레시피 ID가 비어있습니다")
        }
    }

    private fun bindViews() {
        Log.d(TAG, "bindViews 실행")
        titleEdit = findViewById(R.id.editTextTitle)
        descEdit = findViewById(R.id.making_des)
        noteEdit = findViewById(R.id.note_des)
        imageView = findViewById(R.id.imageView22)
        imageBtn = findViewById(R.id.buttonSelectImage)
        uploadBtn = findViewById(R.id.buttonUpload)
        categorySpinner1 = findViewById(R.id.categorySpinner)
        categorySpinner2 = findViewById(R.id.categorySpinner2)
        ingreRecycler = findViewById(R.id.AddPageIngredientsItemList)
        tagRecycler = findViewById(R.id.AddPageItemTags)
        stageRecycler = findViewById(R.id.AddPageStageListRecyclerView)
        ingreField = findViewById(R.id.ingre_textField)
        ingreAddBtn = findViewById(R.id.ingre_btn)
        tagField = findViewById(R.id.tags_inputTextField)
        tagAddBtn = findViewById(R.id.tags_inputButton)
        stageTitle = findViewById(R.id.AddPageStageTitleText)
        stageDesc = findViewById(R.id.AddPageStageDescriptionText)
        stageAddBtn = findViewById(R.id.AddPageStageAddBtn)
        timerSwitch = findViewById(R.id.timerSwitch)
        timerLayout = findViewById(R.id.timerStage)
        timerHour = findViewById(R.id.AddPageStageTimerHour)
        timerMin = findViewById(R.id.AddPageStageTimerMinute)
        timerSec = findViewById(R.id.AddPageStageTimerSecond)
        methodEdit = findViewById(R.id.addpageCookingMethod)

        timerLayout.visibility = View.GONE
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners 실행")

        imageBtn.setOnClickListener {
            Log.d(TAG, "이미지 선택 버튼 클릭됨")
            imagePicker.launch("image/*")
        }

        ingreAddBtn.setOnClickListener {
            val input = ingreField.text.toString().trim()
            if (input.isNotEmpty()) {
                ingredients.add(input)
                ingreField.setText("")
                ingreAdapter.notifyDataSetChanged()
                Log.d(TAG, "재료 추가됨: $input")
            }
        }

        tagAddBtn.setOnClickListener {
            val input = tagField.text.toString().trim()
            if (input.isNotEmpty()) {
                tags.add("#$input")
                tagField.setText("")
                tagAdapter.notifyDataSetChanged()
                Log.d(TAG, "태그 추가됨: #$input")
            }
        }

        timerSwitch.setOnCheckedChangeListener { _, isChecked ->
            timerLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            Log.d(TAG, "타이머 스위치 변경됨: $isChecked")
        }

        stageAddBtn.setOnClickListener {
            val stepTitle = stageTitle.text.toString()
            val stepDesc = stageDesc.text.toString()
            val cookingMethod = methodEdit.text.toString()
            val time = getFormattedTime()

            if (stepTitle.isNotEmpty() && stepDesc.isNotEmpty()) {
                val formatted = if (timerSwitch.isChecked && cookingMethod.isNotEmpty())
                    "○${stepTitle}. ${stepDesc} (${cookingMethod},${time})"
                else
                    "○${stepTitle}. ${stepDesc}"

                steps.add(formatted)
                stepAdapter.updateItems(steps)
                Log.d(TAG, "단계 추가됨: $formatted")

                stageTitle.setText("")
                stageDesc.setText("")
                methodEdit.setText("")
                timerHour.setText("")
                timerMin.setText("")
                timerSec.setText("")
                timerSwitch.isChecked = false
            }
        }

        uploadBtn.setOnClickListener {
            Log.d(TAG, "업로드 버튼 클릭됨")
            if (imageUri != null) {
                FireStoreHelper.uploadImage(
                    imageUri!!,
                    "recipe_$recipeId",
                    "recipe_image",
                    { url -> saveRecipe(url) },
                    {
                        Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "이미지 업로드 실패")
                    }
                )
            } else {
                saveRecipe(originalImageUrl)
            }
        }
    }

    private fun getFormattedTime(): String {
        val h = timerHour.text.toString().padStart(2, '0')
        val m = timerMin.text.toString().padStart(2, '0')
        val s = timerSec.text.toString().padStart(2, '0')
        return "$h:$m:$s"
    }

    private fun setupAdapters() {
        Log.d(TAG, "setupAdapters 실행")
        ingreAdapter = IngredientAdapter(ingredients, this) { }
        tagAdapter = FlexAdaptor(tags)
        stepAdapter = RecipeDetailRecyclerAdaptor(mutableListOf(), this) { }

        ingreRecycler.layoutManager = LinearLayoutManager(this)
        ingreRecycler.adapter = ingreAdapter

        tagRecycler.layoutManager = LinearLayoutManager(this)
        tagRecycler.adapter = tagAdapter

        stageRecycler.layoutManager = LinearLayoutManager(this)
        stageRecycler.adapter = stepAdapter
    }

    private fun setupSpinners() {
        Log.d(TAG, "setupSpinners 실행")

        val categoryMapping = mapOf(
            "C_food_types" to categorySpinner1,
            "C_cooking_methods" to categorySpinner2
        )

        for ((docName, spinner) in categoryMapping) {
            db.collection("C_categories").document(docName)
                .get()
                .addOnSuccessListener { document ->
                    val list = document.get("list") as? List<String>
                    if (!list.isNullOrEmpty()) {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
                        spinner.adapter = adapter
                        Log.d(TAG, "$docName 스피너 데이터 로드 성공: $list")
                    } else {
                        Log.w(TAG, "$docName 항목이 비어 있음")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "$docName 로드 실패: ${e.message}")
                }
        }
    }

    private fun loadRecipe() {
        Log.d(TAG, "loadRecipe 실행")
        try {
            db.collection("recipe").document(recipeId)
                .get()
                .addOnSuccessListener { doc ->
                    try {
                        val recipe = doc.toObject(RecipeItem::class.java)
                        if (recipe != null) {
                            Log.d(TAG, "레시피 데이터 로드 성공")
                            titleEdit.setText(recipe.name)
                            descEdit.setText(recipe.description)
                            noteEdit.setText(recipe.note)
                            Glide.with(this).load(recipe.imageResId).into(imageView)
                            originalImageUrl = recipe.imageResId

                            ingredients.clear()
                            ingredients.addAll(recipe.ingredients)
                            ingreAdapter.notifyDataSetChanged()

                            tags.clear()
                            tags.addAll(recipe.tags)
                            tagAdapter.notifyDataSetChanged()

                            recipe.C_categories.getOrNull(0)?.let { setSpinnerSelection(categorySpinner1, it) }
                            recipe.C_categories.getOrNull(1)?.let { setSpinnerSelection(categorySpinner2, it) }

                            steps.clear()
                            try {
                                val recipeSteps = recipe.order.split("○").filter { it.isNotBlank() }
                                Log.d(TAG, "원본 레시피 단계: $recipeSteps")

                                steps.addAll(recipeSteps.mapIndexed { index, step ->
                                    try {
                                        val cleanStep = step.trim()
                                        if (cleanStep.contains("(") && cleanStep.contains(")")) {
                                            "○$cleanStep"
                                        } else {
                                            val stepNumber = index + 1
                                            val stepContent = cleanStep.substringAfter(".")
                                                .substringAfter(" ")
                                                .trim()
                                            "○$stepNumber.($stepContent)"
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "단계 변환 중 오류: ${e.message}")
                                        "○${index + 1}.(${step.trim()})"
                                    }
                                })
                                Log.d(TAG, "변환된 레시피 단계: $steps")
                                stepAdapter.updateItems(steps)
                            } catch (e: Exception) {
                                Log.e(TAG, "레시피 단계 처리 중 오류: ${e.message}")
                                Toast.makeText(this, "레시피 단계 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e(TAG, "레시피 데이터가 null입니다")
                            Toast.makeText(this, "레시피 데이터를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "레시피 데이터 처리 중 오류: ${e.message}")
                        Toast.makeText(this, "레시피 데이터 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "레시피 로드 실패: ${e.message}")
                    Toast.makeText(this, "레시피 로드에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "loadRecipe 실행 중 오류: ${e.message}")
            Toast.makeText(this, "예기치 않은 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == value) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun saveRecipe(imageUrl: String) {
        Log.d("EditRecipeActivity", "레시피 저장 시도 중... recipeId: $recipeId")

        val updatedData = mapOf(
            "name" to titleEdit.text.toString(),
            "description" to descEdit.text.toString(),
            "note" to noteEdit.text.toString(),
            "imageResId" to imageUrl,
            "ingredients" to ingredients,
            "tags" to tags,
            "order" to steps.joinToString(""),
            "C_categories" to listOf(
                categorySpinner1.selectedItem.toString(),
                categorySpinner2.selectedItem.toString()
            )
        )

        db.collection("recipe").document(recipeId)
            .set(updatedData, SetOptions.merge())  // << 여기!
            .addOnSuccessListener {
                Toast.makeText(this, "레시피 수정 완료", Toast.LENGTH_SHORT).show()
                Log.d("EditRecipeActivity", "레시피 수정 완료")
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "수정 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditRecipeActivity", "레시피 수정 실패: ${it.message}")
            }
    }
}

