package com.bcu.foodtable.useful

data class GalleryItem(
    val userId : String="",
    val recipeId:String="",
    val groupId:String="", // 그룹핑을 위한 ID
    var name: String="",  // 레시피 또는 그룹 대표 아이템의 이름
    var image: String? = null, // 이미지 URL (String? 타입 권장)
    val creationTimestamp: Long? = 0L, // 정렬 등을 위한 타임스탬프 (Long? 타입 권장)
    val groupName: String? = null // 그룹의 표시 이름 (String? 타입 권장, groupId와 다를 수 있음)
    // 여기에 사용자 앱에 필요한 다른 필드들이 있다면 추가/유지하세요.
)