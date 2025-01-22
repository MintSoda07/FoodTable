package com.bcu.foodtable.useful

//  User 데이터타입을 초기화하고 사용하기 위한 object
object UserManager {
    private var user: User? = null

    fun setUser(name: String, email: String, imageURL: String, phoneNumber: String, point: Int) {
        user = User(name, email, imageURL, phoneNumber, point)
    }

    fun getUser(): User? {
        return user
    }
}