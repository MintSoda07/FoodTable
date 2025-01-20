package com.bcu.foodtable.useful

//  User 데이터타입을 초기화하고 사용하기 위한 object
object UserManager {
    private var user: User? = null

    fun setUser(name: String, email: String, phoneNumber: String,point:Int ,imageID:Int) {
        user = User(name, email, phoneNumber, point, imageID)
    }

    fun getUser(): User? {
        return user
    }
}