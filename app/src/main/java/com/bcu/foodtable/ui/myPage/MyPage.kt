package com.bcu.foodtable.ui.myPage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bcu.foodtable.PuchasePage
import com.bcu.foodtable.databinding.FragmentMypageBinding
import com.bcu.foodtable.ui.health.HealthConnectActivity
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.FireStoreHelper.loadImageFromUrl
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.bcu.foodtable.ui.myPage.ChannelCreationActivity

class MyPage : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    private var originalDescription: String = ""
    private var originalImageUrl: String = ""
    private var tempSelectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        fetchUserDataFromFirestore()
        toggleEditMode(false)
        // 채널 존재 여부 체크
        checkIfChannelExists()
        // "채널 생성하기" 버튼 클릭 리스너 추가
        binding.ProfileCreateChannelBtn.setOnClickListener {
            // 채널 생성 페이지로 이동
            binding.ProfileCreateChannelBtn.setOnClickListener {
                // 채널 생성 페이지로 이동
                val intent = Intent(context, ChannelCreationActivity::class.java)
                startActivity(intent)
            }
        }

        binding.ProfileEditBtn.setOnClickListener {
            toggleEditMode(true)
        }

        binding.ProfileSaveBtn.setOnClickListener {
            saveProfileChanges()
        }

        binding.ProfileCancelBtn.setOnClickListener {
            tempSelectedImageUri = null
            loadImageFromUrl(originalImageUrl, binding.ProfileMyImage)
            toggleEditMode(false)
        }

        binding.ProfileMyImage.setOnClickListener {
            if (binding.ProfileMyImage.isClickable) {
                pickImageFromGallery()
            }
        }

        binding.ProfileCameraIcon.setOnClickListener {
            pickImageFromGallery()
        }

        binding.ProfileSaltPurchaseBtn.setOnClickListener{
            ActivityTransition.startStaticInFragment(
                context,
                PuchasePage::class.java
            )
        }
        // 헬스 커넥트 액티비티 연결
        binding.ProfileStepButton.setOnClickListener {
            val intent = Intent(requireContext(), HealthConnectActivity::class.java)
            startActivity(intent)
        }

        return root
    }

    private fun saveProfileChanges() {
        val newDescription = binding.ProfileIntroduceEditText.text.toString()
        val user = UserManager.getUser() ?: return

        FirebaseFirestore.getInstance().collection("user").document(user.uid)
            .update("description", newDescription)
            .addOnSuccessListener {
                Toast.makeText(context, "프로필이 성공적으로 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                binding.ProfileIntroduceTextView.text = newDescription
                toggleEditMode(false)
            }
            .addOnFailureListener {
                Toast.makeText(context, "❌ 프로필 업데이트 실패.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserDataFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("user").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "이름 없음"
                    val description = document.getString("description") ?: ""
                    val point = document.getLong("point")?.toInt() ?: 0
                    val rankPoint = document.getLong("rankPoint")?.toInt() ?: 0
                    val imageUrl = document.getString("image") ?: ""

                    binding.ProfileMyName.text = name
                    binding.ProfileIntroduceTextView.text = if (description.isNotEmpty()) description else "자기소개가 없습니다."
                    binding.ProfileIntroduceEditText.setText(description)
                    binding.ProfileMySalt.text = "$point"
                    binding.ProfileRankText.text = "$rankPoint"

                    if (imageUrl.isNotEmpty()) {
                        originalImageUrl = imageUrl
                        loadImageFromUrl(imageUrl, binding.ProfileMyImage)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "파이어스토어에서 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleEditMode(enable: Boolean) {
        binding.ProfileIntroduceTextView.visibility = if (enable) View.GONE else View.VISIBLE
        binding.ProfileIntroduceEditText.visibility = if (enable) View.VISIBLE else View.GONE

        binding.ProfileEditBtn.visibility = if (enable) View.GONE else View.VISIBLE
        binding.ProfileSaveBtn.visibility = if (enable) View.VISIBLE else View.GONE
        binding.ProfileCancelBtn.visibility = if (enable) View.VISIBLE else View.GONE
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            tempSelectedImageUri = data?.data
            tempSelectedImageUri?.let {
                binding.ProfileMyImage.setImageURI(it)
                uploadImageToFirebase(FirebaseAuth.getInstance().currentUser?.uid ?: "", it)
            }
        }
    }

    private fun uploadImageToFirebase(userId: String, uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("user_profile_image/$userId.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateUserProfileImage(userId, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileImage(userId: String, imageUrl: String) {
        FirebaseFirestore.getInstance().collection("user").document(userId)
            .update("image", imageUrl)
            .addOnSuccessListener {
                originalImageUrl = imageUrl
                loadImageFromUrl(imageUrl, binding.ProfileMyImage)
            }
            .addOnFailureListener {
                Toast.makeText(context, "프로필 이미지 저장 실패.", Toast.LENGTH_SHORT).show()
            }
    }
    private fun checkIfChannelExists() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Firebase Firestore에서 채널 정보 가져오기
        FirebaseFirestore.getInstance().collection("channel")
            .whereEqualTo("userId", uid) // 사용자의 채널이 있는지 확인
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // 채널이 존재하지 않으면 버튼 보이기
                    binding.ProfileCreateChannelBtn.visibility = View.VISIBLE
                } else {
                    // 채널이 이미 존재하면 버튼 숨기기
                    binding.ProfileCreateChannelBtn.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "채널 존재 여부 확인 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
    }
}
