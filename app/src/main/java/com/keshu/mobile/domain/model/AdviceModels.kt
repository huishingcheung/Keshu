package com.keshu.mobile.domain.model

data class CourseAdvice(
    val targetTerm: String,
    val summary: String,
    val recommendations: List<CourseRecommendation>,
)

data class CourseRecommendation(
    val courseCode: String,
    val courseName: String,
    val groupName: String,
    val credit: Double,
    val priority: String,
    val reason: String,
    val feedback: String,
)
