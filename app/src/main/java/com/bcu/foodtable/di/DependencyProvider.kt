package com.bcu.foodtable.di

import android.content.Context
import com.bcu.foodtable.ai.AIRecommendationService
import com.bcu.foodtable.data.UserBehaviorTracker
import com.bcu.foodtable.manager.TimeBasedRecommendationManager
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.google.firebase.firestore.FirebaseFirestore

/**
 * 수동 DI를 위한 의존성 제공자 클래스
 * 앱 전체에서 사용할 의존성들을 중앙에서 관리합니다.
 */
class DependencyProvider private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: DependencyProvider? = null
        
        fun getInstance(): DependencyProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DependencyProvider().also { INSTANCE = it }
            }
        }
    }
    
    // 🔧 의존성 캐시 (싱글톤 패턴)
    private var _userBehaviorTracker: UserBehaviorTracker? = null
    private var _aiRecommendationService: AIRecommendationService? = null
    private var _timeBasedRecommendationManager: TimeBasedRecommendationManager? = null
    
    /**
     * 🔍 사용자 행동 추적기를 제공합니다.
     * @param context 애플리케이션 컨텍스트
     * @return UserBehaviorTracker 인스턴스
     */
    fun provideUserBehaviorTracker(context: Context): UserBehaviorTracker {
        return _userBehaviorTracker ?: synchronized(this) {
            _userBehaviorTracker ?: UserBehaviorTracker(
                context = context.applicationContext,
                firestore = FirebaseFirestore.getInstance()
            ).also { _userBehaviorTracker = it }
        }
    }
    
    /**
     * 🤖 AI 추천 서비스를 제공합니다.
     * @return AIRecommendationService 인스턴스
     */
    fun provideAIRecommendationService(): AIRecommendationService {
        return _aiRecommendationService ?: synchronized(this) {
            _aiRecommendationService ?: AIRecommendationService().also { 
                _aiRecommendationService = it 
            }
        }
    }
    
    /**
     * ⏰ 시간 기반 추천 관리자를 제공합니다.
     * @param context 애플리케이션 컨텍스트
     * @return TimeBasedRecommendationManager 인스턴스
     */
    fun provideTimeBasedRecommendationManager(context: Context): TimeBasedRecommendationManager {
        return _timeBasedRecommendationManager ?: synchronized(this) {
            _timeBasedRecommendationManager ?: TimeBasedRecommendationManager(
                aiService = provideAIRecommendationService(),
                behaviorTracker = provideUserBehaviorTracker(context)
            ).also { _timeBasedRecommendationManager = it }
        }
    }
    
    /**
     * 🏠 HomeViewModel을 제공합니다.
     * @param context 애플리케이션 컨텍스트
     * @return HomeViewModel 인스턴스
     */
    fun provideHomeViewModel(context: Context): HomeViewModel {
        return HomeViewModel(
            db = FirebaseFirestore.getInstance(),
            behaviorTracker = provideUserBehaviorTracker(context),
            aiService = provideAIRecommendationService(),
            timeManager = provideTimeBasedRecommendationManager(context)
        )
    }
    
    /**
     * 🧹 의존성 캐시를 초기화합니다. (테스트나 메모리 정리 시 사용)
     */
    fun clearCache() {
        synchronized(this) {
            _userBehaviorTracker = null
            _aiRecommendationService = null
            _timeBasedRecommendationManager = null
        }
    }
    
    /**
     * 📊 현재 캐시된 의존성들의 상태를 로깅합니다. (디버깅용)
     */
    fun logDependencyStatus() {
        android.util.Log.d("DependencyProvider", """
            📊 의존성 상태:
            🔍 UserBehaviorTracker: ${if (_userBehaviorTracker != null) "생성됨" else "미생성"}
            🤖 AIRecommendationService: ${if (_aiRecommendationService != null) "생성됨" else "미생성"}
            ⏰ TimeBasedRecommendationManager: ${if (_timeBasedRecommendationManager != null) "생성됨" else "미생성"}
        """.trimIndent())
    }
} 