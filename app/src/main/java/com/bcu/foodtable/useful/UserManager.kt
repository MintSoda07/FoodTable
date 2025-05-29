package com.bcu.foodtable.useful

//  User 데이터타입을 초기화하고 사용하기 위한 object
object UserManager {
    private var user: User? = null

    fun setUser(
        name: String,
        email: String,
        imageURL: String,
        phoneNumber: String,
        point: Int,
        uid: String,
        rankPoint: Int,
        description: String,
        location : String,
        manager : Boolean
    ) {
        user = User(name, email, imageURL, phoneNumber, point, uid,rankPoint,description,location,manager)
    }
    fun setUserByDatatype(userdata:User){
        user = User(userdata.name, userdata.email, userdata.image, userdata.phoneNumber, userdata.point, userdata.uid,userdata.rankPoint,userdata.description, userdata.location, userdata.manager)
    }

    fun getUser(): User? {
        return user
    }
}