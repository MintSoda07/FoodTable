package com.bcu.foodtable.useful

//  ApiKey 데이터타입을 초기화하고 사용하기 위한 object
//  현재 GPT_API만을 관리하고 있으나 다른 키를 넣어도 됨.

object ApiKeyManager {
    private var gptApi: ApiKey? = null

    fun setGptApiKey(name:String,value:String) {
        gptApi= ApiKey(name,value)
    }

    fun getGptApi(): ApiKey? {
        return gptApi
    }
}