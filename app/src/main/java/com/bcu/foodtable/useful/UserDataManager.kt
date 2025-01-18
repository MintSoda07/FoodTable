package com.bcu.foodtable.useful

object UserManager {
    private var user: User? = null

    fun setUser(name: String, email: String, phoneNumber: String,point:Int ,imageID:Int) {
        user = User(name, email, phoneNumber, point, imageID)
    }

    fun getUser(): User? {
        return user
    }
}