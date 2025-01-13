package com.bcu.foodtable.useful

object UserManager {
    private var user: User? = null

    fun setUser(name: String, email: String, phoneNumber: Int) {
        user = User(name, email, phoneNumber)
    }

    fun getUser(): User? {
        return user
    }
}