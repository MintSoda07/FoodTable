package com.bcu.foodtable.ui.myPage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bcu.foodtable.databinding.FragmentMypageBinding
import com.bcu.foodtable.useful.FireStoreHelper.loadImageFromUrl
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MyPage : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val userData = UserManager.getUser()
        if (userData != null) {
            loadImageFromUrl(userData.image, binding.ProfileMyImage)
            binding.ProfileMyName.text = userData.Name
            binding.ProfileIntroduceEditText.setText(userData.description)
            binding.ProfileMySalt.text = "${userData.point}"
            binding.ProfileRankText.text = "${userData.rankPoint}"
            toggleEditMode(false) // 초기에는 읽기 전용
        } else {
            fetchUserDataFromFirestore()
        }

        binding.ProfileEditBtn.setOnClickListener {
            toggleEditMode(true)
        }

        binding.ProfileSaveBtn.setOnClickListener {
            saveProfileChanges()
        }

        binding.ProfileCancelBtn.setOnClickListener {
            toggleEditMode(false)
        }

        binding.ProfileMyImage.setOnClickListener {
            pickImageFromGallery()
        }

        return root
    }

    private fun fetchUserDataFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        println(" Firestore에서 사용자 데이터 가져오는 중: $uid")
        FirebaseFirestore.getInstance().collection("user").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "이름 없음"
                    println(" Firestore에서 가져온 이름: $name")
                    val description = document.getString("description") ?: ""
                    val point = document.getLong("point")?.toInt() ?: 0
                    val rankPoint = document.getLong("rankPoint")?.toInt() ?: 0
                    val imageUrl = document.getString("image") ?: ""

                    requireActivity().runOnUiThread {
                        binding.ProfileMyName.text = name
                        binding.ProfileIntroduceEditText.setText(description)
                        binding.ProfileMySalt.text = "$point"
                        binding.ProfileRankText.text = "$rankPoint"
                        loadImageFromUrl(imageUrl, binding.ProfileMyImage)
                    }
                } else {
                    println(" Firestore 문서가 존재하지 않음")
                }
            }

            .addOnFailureListener {
                println(" Firestore에서 데이터 가져오기 실패: ${it.message}")
                Toast.makeText(context, "파이어스토어에서 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleEditMode(enable: Boolean) {
        binding.ProfileIntroduceEditText.visibility = if (enable) View.VISIBLE else View.GONE
        binding.ProfileIntroduceEditText.isEnabled = enable
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
            selectedImageUri = data?.data
            selectedImageUri?.let {
                binding.ProfileMyImage.setImageURI(it)
            }
        }
    }

    private fun saveProfileChanges() {
        val newDescription = binding.ProfileIntroduceEditText.text.toString()
        val user = UserManager.getUser() ?: return

        val updates = mutableMapOf<String, Any>()
        updates["description"] = newDescription

        FirebaseFirestore.getInstance().collection("user").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "프로필이 성공적으로 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                binding.ProfileMyName.text = user.Name // 사용자 이름 업데이트 반영
                toggleEditMode(false)
            }
            .addOnFailureListener {
                Toast.makeText(context, "프로필 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }

        if (selectedImageUri != null) {
            uploadImageToFirebase(user.uid)
        }
    }

    private fun uploadImageToFirebase(userId: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("user_profile_image/$userId.jpg")
        selectedImageUri?.let {
            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        updateUserProfileImage(userId, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserProfileImage(userId: String, imageUrl: String) {
        FirebaseFirestore.getInstance().collection("user").document(userId)
            .update("image", imageUrl)
            .addOnSuccessListener {
                Toast.makeText(context, "프로필 이미지가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                loadImageFromUrl(imageUrl, binding.ProfileMyImage)
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
