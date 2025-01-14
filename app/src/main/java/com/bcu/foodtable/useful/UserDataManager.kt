package com.bcu.foodtable.useful

object UserManager {
    private var user: User? = null

    fun setUser(name: String, email: String, phoneNumber: String,point:Int) {
        user = User(name, email, phoneNumber, point)
    }

    fun getUser(): User? {
        return user
    }
}